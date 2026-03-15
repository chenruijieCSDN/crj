package com.yupi.yuaiagent.agent;

import com.itextpdf.styledxmlparser.jsoup.internal.StringUtil;
import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 *
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    /** 单步执行超时时间（秒），避免大上下文时模型响应过慢导致“卡住”无反馈 */
    private static final int STEP_TIMEOUT_SECONDS = 120;

    private static final ExecutorService stepExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "agent-step");
        t.setDaemon(true);
        return t;
    });

    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    private String nextStepPrompt;

    // 状态
    private AgentState state = AgentState.IDLE;

    // 执行控制
    private int maxSteps = 10;
    private int currentStep = 0;

    // LLM
    private ChatClient chatClient;

    // Memory（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            // 若有最终助手回复，优先返回其文本，否则返回步骤摘要
            String finalText = getLastAssistantText();
            if (finalText != null && !finalText.isBlank()) {
                return finalText;
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return SseEmitter实例
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter，设置较长的超时时间
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    if (!sendOrDetectDisconnect(emitter, "错误：无法从状态运行代理: " + this.state)) return;
                    emitter.complete();
                    return;
                }
                if (StringUtil.isBlank(userPrompt)) {
                    if (!sendOrDetectDisconnect(emitter, "错误：不能使用空提示词运行代理")) return;
                    emitter.complete();
                    return;
                }

                // 更改状态
                state = AgentState.RUNNING;
                // 记录消息上下文
                messageList.add(new UserMessage(userPrompt));

                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);

                        // 单步执行（带超时，避免大上下文时模型长时间无响应）
                        String stepResult;
                        Future<String> future = stepExecutor.submit(this::step);
                        try {
                            stepResult = future.get(STEP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            future.cancel(true);
                            log.warn("Step {} timed out after {}s", stepNumber, STEP_TIMEOUT_SECONDS);
                            if (!sendOrDetectDisconnect(emitter, "步骤 " + stepNumber + " 执行超时（超过 " + STEP_TIMEOUT_SECONDS + " 秒）。请重试或简化任务描述。")) break;
                            break;
                        } catch (ExecutionException e) {
                            Throwable cause = e.getCause();
                            log.error("Step " + stepNumber + " failed", cause);
                            if (!sendOrDetectDisconnect(emitter, "步骤 " + stepNumber + " 执行异常: " + (cause != null ? cause.getMessage() : e.getMessage()))) break;
                            break;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            future.cancel(true);
                            log.warn("Step {} interrupted", stepNumber);
                            if (!sendOrDetectDisconnect(emitter, "步骤 " + stepNumber + " 被中断。")) break;
                            break;
                        }

                        String result = "Step " + stepNumber + ": " + stepResult;

                        // 发送每一步的结果（客户端断开则不再继续发送）
                        if (!sendOrDetectDisconnect(emitter, result)) break;
                    }
                    // 检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        sendOrDetectDisconnect(emitter, "执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    // 正常完成
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        sendOrDetectDisconnect(emitter, "执行错误: " + e.getMessage());
                        emitter.complete();
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    // 清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });

        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }

    /**
     * 向 SSE 客户端发送数据；若客户端已断开（IOException）则仅记录日志并返回 false。
     *
     * @param emitter SseEmitter
     * @param data    要发送的字符串
     * @return 发送成功为 true，客户端已断开为 false
     */
    protected boolean sendOrDetectDisconnect(SseEmitter emitter, String data) {
        try {
            emitter.send(data);
            return true;
        } catch (IOException e) {
            log.debug("SSE client disconnected: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 执行单个步骤
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 获取会话中最后一条助手消息的文本（用于 run 结束时返回最终回答）
     */
    protected String getLastAssistantText() {
        List<Message> list = getMessageList();
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            Message msg = list.get(i);
            if (msg instanceof AssistantMessage) {
                String text = ((AssistantMessage) msg).getText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }
}

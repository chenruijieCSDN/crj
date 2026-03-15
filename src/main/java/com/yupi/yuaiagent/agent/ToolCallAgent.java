package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI 手动控制工具执行的 ReAct 代理。
 * 继承 ReActAgent，实现 think（调用模型、判断是否有工具调用）和 act（执行工具、更新会话）。
 * 使用 ToolCallingChatOptions.internalToolExecutionEnabled(false) + ToolCallingManager.executeToolCalls 自主完成 think → act → observe 循环。
 */
@Slf4j
@Getter
public class ToolCallAgent extends ReActAgent {

    private final ChatModel chatModel;
    private final ToolCallback[] availableTools;
    private final ToolCallingManager toolCallingManager;
    private final ToolCallingChatOptions chatOptions;

    /**
     * 最近一次模型调用的 Prompt（供 act 中 executeToolCalls 使用）
     */
    private Prompt lastPrompt;
    /**
     * 最近一次模型响应（若包含 tool calls 则供 act 执行）
     */
    private ChatResponse toolCallChatResponse;

    public ToolCallAgent(ChatModel chatModel, ToolCallback[] availableTools) {
        super();
        this.chatModel = chatModel;
        this.availableTools = availableTools != null ? availableTools : new ToolCallback[0];
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(this.availableTools)
                .internalToolExecutionEnabled(false)
                .build();
    }

    /**
     * think：用当前会话调用模型，若返回中有工具调用则返回 true，否则结束并返回 false。
     */
    @Override
    public boolean think() {
        List<Message> messages = new ArrayList<>();
        if (getSystemPrompt() != null && !getSystemPrompt().isBlank()) {
            messages.add(new SystemMessage(getSystemPrompt()));
        }
        messages.addAll(getMessageList());

        lastPrompt = new Prompt(messages, chatOptions);
        log.info("Calling chat model for step {} (messages={})", getCurrentStep(), messages.size());
        toolCallChatResponse = chatModel.call(lastPrompt);

        if (toolCallChatResponse == null || toolCallChatResponse.getResult() == null) {
            log.warn("模型返回为空");
            setState(AgentState.FINISHED);
            return false;
        }

        if (toolCallChatResponse.hasToolCalls()) {
            AssistantMessage assistantMessage = toolCallChatResponse.getResult().getOutput();
            String thinking = assistantMessage.getText();
            if (thinking != null && !thinking.isBlank()) {
                log.info("{}的思考: {}", getName(), thinking);
            }
            var toolCalls = assistantMessage.getToolCalls();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.info("{}选择了 {} 个工具来使用", getName(), toolCalls.size());
                String toolCallInfo = toolCalls.stream()
                        .map(tc -> String.format("工具名称:%s，参数:%s", tc.name(), tc.arguments()))
                        .collect(Collectors.joining("\n"));
                log.info(toolCallInfo);
            }
            log.info("模型请求执行工具调用");
            return true;
        }

        // 无工具调用：将助手回复加入会话并结束
        Message output = toolCallChatResponse.getResult().getOutput();
        if (output != null) {
            getMessageList().add(output);
        }
        setState(AgentState.FINISHED);
        return false;
    }

    /**
     * act：使用 ToolCallingManager 执行上一轮 think 中的工具调用，并用执行结果更新会话（observe）。
     */
    @Override
    public String act() {
        if (lastPrompt == null || toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return "无待执行的工具调用";
        }
        try {
            ToolExecutionResult result = toolCallingManager.executeToolCalls(lastPrompt, toolCallChatResponse);
            List<Message> conversationHistory = result.conversationHistory();
            StringBuilder output = new StringBuilder();
            if (conversationHistory != null && !conversationHistory.isEmpty()) {
                getMessageList().clear();
                getMessageList().addAll(conversationHistory);
                Message lastMessage = conversationHistory.get(conversationHistory.size() - 1);
                if (lastMessage instanceof ToolResponseMessage trm && trm.getResponses() != null) {
                    for (var r : trm.getResponses()) {
                        log.info("工具 {} 返回的结果: {}", r.name(), r.responseData());
                        String data = r.responseData() != null ? r.responseData().toString() : "";
                        if (!data.isBlank()) {
                            // searchWeb 返回大段 JSON，仅向前端展示简短提示，避免刷屏
                            String toAppend = formatToolOutputForFrontend(r.name(), data.trim());
                            if (!toAppend.isBlank()) {
                                output.append(toAppend).append("\n\n");
                            }
                        }
                    }
                }
            }
            int count = toolCallChatResponse.getResult().getOutput().getToolCalls() != null
                    ? toolCallChatResponse.getResult().getOutput().getToolCalls().size()
                    : 0;
            String summary = "已执行 " + count + " 个工具调用并更新会话（observe）";
            if (output.length() > 0) {
                return summary + "\n\n" + output.toString().trim();
            }
            return summary;
        } catch (Exception e) {
            log.error("执行工具调用失败", e);
            return "工具执行失败: " + e.getMessage();
        }
    }

    /**
     * 格式化工具输出再发给前端：大段内容只发简短提示，避免前端刷屏。
     * - searchWeb：大段 JSON → 简短提示
     * - generatePDF：含【计划正文】的长文 → 保留「PDF generated successfully to: path」供下载，正文改为简短提示
     */
    private String formatToolOutputForFrontend(String toolName, String data) {
        if (data == null || data.isBlank()) return "";
        if ("searchWeb".equals(toolName) && data.length() > 400) {
            return "已获取网页搜索结果（供模型参考，已用于后续步骤）";
        }
        // PDF 成功返回带长计划正文时，只保留路径行 + 简短说明，便于前端显示「下载 PDF」且不刷屏
        if (("generatePDF".equals(toolName) || data.contains("PDF generated successfully to:"))
                && data.contains("【计划正文】")) {
            String pathLine = null;
            int start = data.indexOf("PDF generated successfully to:");
            if (start >= 0) {
                int end = data.indexOf("\n", start);
                pathLine = end > start ? data.substring(start, end).trim() : data.substring(start).trim();
            }
            if (pathLine != null) {
                return pathLine + "\n\n完整计划已写入 PDF，请下载查看。";
            }
        }
        return data;
    }
}

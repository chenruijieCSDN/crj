package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.YuManus;
import com.yupi.yuaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * AI 应用统一接口：恋爱大师同步/流式对话。
 * 参考：<a href="https://github.com/liyupi/yu-ai-agent">yu-ai-agent</a>
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    /** POST /manus/chat 请求体 */
    public record ManusChatRequest(String message) {}

    @Resource
    private LoveApp loveApp;

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    private static final String NO_API_KEY_MSG = "未配置 DASHSCOPE_API_KEY，请在环境变量或 application-local.yml 中设置 spring.ai.dashscope.api-key。";

    /**
     * 同步调用 AI 恋爱大师
     *
     * @param message 用户消息
     * @param chatId  对话ID（多轮记忆，可选）
     * @return 完整回复文本
     */
    @GetMapping("/love_app/chat/sync")
    public ResponseEntity<String> doChatWithLoveAppSync(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", required = false, defaultValue = "default") String chatId) {
        if (!StringUtils.hasText(dashscopeApiKey)) {
            return ResponseEntity.status(503).body(NO_API_KEY_MSG);
        }
        String result = loveApp.doChat(message, chatId);
        return ResponseEntity.ok(result != null ? result : "");
    }

    /**
     * SSE 流式调用（直接返回 Flux，Spring 自动按 text/event-stream 推送）
     *
     * @param message 用户消息
     * @param chatId  对话ID（可选）
     * @return 流式文本
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", required = false, defaultValue = "default") String chatId) {
        if (!StringUtils.hasText(dashscopeApiKey)) {
            return Flux.just("data: " + NO_API_KEY_MSG + "\n\n");
        }
        return loveApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用（封装为 ServerSentEvent，便于前端按事件解析）
     *
     * @param message 用户消息
     * @param chatId  对话ID（可选）
     * @return SSE 事件流
     */
    @GetMapping(value = "/love_app/chat/server_sent_event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", required = false, defaultValue = "default") String chatId) {
        if (!StringUtils.hasText(dashscopeApiKey)) {
            return Flux.just(ServerSentEvent.<String>builder().data(NO_API_KEY_MSG).build());
        }
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用（使用 SseEmitter，通过 send 持续推送，类似 IO 操作）
     *
     * @param message 用户消息
     * @param chatId  对话ID（可选）
     * @return SseEmitter，订阅 Flux 并持续 send 到客户端
     */
    @GetMapping(value = "/love_app/chat/sse_emitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithLoveAppSseEmitter(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", required = false, defaultValue = "default") String chatId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 分钟超时
        if (!StringUtils.hasText(dashscopeApiKey)) {
            try {
                emitter.send(NO_API_KEY_MSG);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
            return emitter;
        }
        loveApp.doChatByStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 流式调用 Manus 超级智能体（GET，SSE 按步推送）
     * 注意：中文等非 ASCII 需 URL 编码，如 你好 → %E4%BD%A0%E5%A5%BD，或改用 POST /manus/chat。
     */
    @GetMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithManusGet(@RequestParam("message") String message) {
        return doChatWithManus(message);
    }

    /**
     * 流式调用 Manus 超级智能体（POST JSON body，推荐：可直接传中文，无需 URL 编码）
     *
     * @param request 请求体，如 {"message": "你好"}，body 可为空
     * @return SseEmitter，按步骤推送 "Step N: ..."
     */
    @PostMapping(value = "/manus/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public SseEmitter doChatWithManusPost(@RequestBody(required = false) ManusChatRequest request) {
        String message = request != null ? request.message() : null;
        return doChatWithManus(message);
    }

    private SseEmitter doChatWithManus(String message) {
        if (!StringUtils.hasText(dashscopeApiKey)) {
            SseEmitter emitter = new SseEmitter(5000L);
            try {
                emitter.send(NO_API_KEY_MSG);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
            return emitter;
        }
        if (!StringUtils.hasText(message)) {
            SseEmitter emitter = new SseEmitter(5000L);
            try {
                emitter.send("错误：请提供任务描述（message 参数不能为空）");
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
            return emitter;
        }
        YuManus yuManus = new YuManus(dashscopeChatModel, allTools);
        return yuManus.runStream(message);
    }
}

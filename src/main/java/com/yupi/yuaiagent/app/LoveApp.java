package com.yupi.yuaiagent.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.advisor.PgVectorRagAdvisor;
import com.yupi.yuaiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;


@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    public LoveApp(DashScopeChatModel dashscopeChatModel) {
        //基于内存的对话记忆
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志记录器，可按需开启
                        new MyLoggerAdvisor())
                        /*// 自定义重新读取用户输入，可按需开启
                        new ReReadingAdvisor()*/
                .build();

    }


    /**
     * AI 基础对话（支持多轮对话）
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String text = null;
        if (chatResponse != null) {
            text = chatResponse.getResult().getOutput().getText();
        }

        log.info("content: {}",text);
        return text;
    }

    /**
     * AI 恋爱大师流式对话（返回 Flux，用于 SSE 等流式输出）
     *
     * @param message 用户消息
     * @param chatId  对话ID（多轮记忆）
     * @return 流式文本块
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record LoveReport(String title, List<String> suggestions) {
    }


    /**
     * 生成恋爱报告（实战结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // === RAG 相关依赖注入 ===
    // 本地SimpleVectorStore知识库（已加载恋爱相关文档）
    @Resource
    private VectorStore loveAppVectorStore;

    // 阿里云知识库服务Advisor
    @Resource
    private Advisor loveAppRagCloudAdvisor;

    // PgVector向量数据库存储（可选：无 PostgreSQL 时为 null，RAG 使用阿里云知识库）
    @Autowired(required = false)
    @Qualifier("pgVectorVectorStore")
    private VectorStore pgVectorVectorStore;


    /**
     * AI RAG对话（基于向量数据库的检索增强）
     *
     * 实现5：错误处理机制 - 允许空上下文（ContextualQueryAugmenter.allowEmptyContext）、
     * 友好错误提示并引导用户提供必要信息
     *
     * 支持多种RAG实现方式：
     * 1. 基于SimpleVectorStore本地知识库 - 使用 loveAppVectorStore
     * 2. 基于阿里云知识库服务 - 当前实现（使用 loveAppRagCloudAdvisor）
     * 3. 基于PgVector向量数据库 - 使用 pgVectorVectorStore（已注释）
     *
     * @param message 用户消息
     * @param chatId 对话ID
     * @return AI回复内容
     */
    public String doChatWithRag(String message, String chatId) {
        try {
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .advisors(loveAppRagCloudAdvisor)
                    .call()
                    .chatResponse();
            if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
                return "抱歉，本次请求未得到有效回复，请稍后重试或换一种方式描述您的问题。";
            }
            String content = chatResponse.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content != null ? content : "未生成回复内容，请补充更多问题描述后重试。";
        } catch (Exception e) {
            log.warn("RAG 对话异常: {}", e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout"))) {
                return "查询超时，请简化问题或稍后重试。";
            }
            if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("no document") || e.getMessage().contains("未找到"))) {
                return "未找到相关参考内容。请尝试换一种说法，或补充更多背景（如：单身/恋爱中/已婚、具体场景），我会基于通用恋爱指导为您解答。";
            }
            return "暂时无法处理您的请求，请稍后重试。若问题持续，请尝试更具体地描述您的困扰。";
        }
    }


    @Resource
    private ToolCallback[] allTools;

    /**
     * 支持工具调用的对话（如：执行终端命令、读写文件、高德地图等）。
     * 同时挂载 Java 注册工具与 MCP 工具（若已启用），便于模型选择高德 MCP 或本地 AmapTool。
     *
     * @param message 用户消息
     * @param chatId  对话ID
     * @return AI 回复文本，异常时返回友好提示
     */
    public String doChatWithTools(String message, String chatId) {
        ToolCallback[] effectiveTools = buildEffectiveTools();
        if (effectiveTools == null || effectiveTools.length == 0) {
            log.warn("未注册任何工具，将进行普通对话");
        }
        try {
            ChatResponse response = chatClient
                    .prompt()
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor())
                    .toolCallbacks(effectiveTools != null ? effectiveTools : new ToolCallback[0])
                    .call()
                    .chatResponse();
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return "本次请求未得到有效回复，请稍后重试。";
            }
            String content = response.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content != null ? content : "未生成回复内容，请重试。";
        } catch (Exception e) {
            log.warn("工具调用对话异常: {}", e.getMessage());
            return "工具调用出错，请稍后重试。若问题持续，请检查网络或简化请求。";
        }
    }


    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 支持 MCP（Model Context Protocol）的对话（如：地图等 MCP 工具）
     *
     * @param message 用户消息
     * @param chatId  对话ID
     * @return AI 回复文本，异常或 MCP 不可用时返回友好提示
     */
    public String doChatWithMcp(String message, String chatId) {
        try {
            var spec = chatClient.prompt()
                    .user(message)
                    .advisors(specParam -> specParam.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(new MyLoggerAdvisor());
            if (toolCallbackProvider != null) {
                spec.toolCallbacks(toolCallbackProvider);
            } else {
                log.warn("MCP ToolCallbackProvider 未注入，本次对话不携带 MCP 工具");
            }
            ChatResponse response = spec.call().chatResponse();
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return "本次请求未得到有效回复，请稍后重试。";
            }
            String content = response.getResult().getOutput().getText();
            log.info("content: {}", content);
            return content != null ? content : "未生成回复内容，请重试。";
        } catch (Exception e) {
            log.warn("MCP 对话异常: {}", e.getMessage());
            return "MCP 服务暂时不可用（请确认 MCP 服务已启动且配置正确），请稍后重试。";
        }
    }

    /**
     * 合并 Java 注册工具与 MCP 工具（若已启用），使「工具对话」既可调本地工具也可调高德等 MCP 服务。
     */
    private ToolCallback[] buildEffectiveTools() {
        ToolCallback[] javaTools = allTools != null ? allTools : new ToolCallback[0];
        if (toolCallbackProvider == null) {
            return javaTools;
        }
        return ToolCallbacks.from(javaTools, toolCallbackProvider);
    }
}

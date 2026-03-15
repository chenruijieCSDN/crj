package com.yupi.yuaiagent.agent;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Manus 智能体：基于 ToolCallAgent 的 ReAct 工具调用 + 人机交互（QueryUserTool、doTerminate）。
 * 参考：
 * - <a href="https://java2ai.com/blog/spring-ai-alibaba-graph-preview">Spring AI Alibaba Graph OpenManus</a>
 * - <a href="https://www.codefather.cn/post/1928476155422310402">智能体Manus改进——人机交互</a>
 */
@Component
public class YuManus extends ToolCallAgent {

    private static final String SYSTEM_PROMPT = """
        你是一个全能的人工智能助手（YuManus），可以调用各种工具来完成用户给你的任务。
        接收到用户的任务后，根据用户要求分析出完成步骤，以第一步、第二步的方式逐步完成。
        为完成用户需求可以调用提供的工具；在得到正确（非报错或未找到）结果后再进行下一步，减少重复调用。
        如果执行任务产生报错或搜索失败，请再次尝试或切换搜索关键词。
        重要：当你已经通过工具获得了足够的信息来回答用户时，必须直接用文字回复总结结果，不要再调用工具。
        """;

    private static final String NEXT_STEP_PROMPT = """
        根据用户需求，主动选择最合适的工具或工具组合。对于复杂任务，可分解问题并逐步使用不同工具解决。
        使用每个工具后，清楚解释执行结果并建议后续步骤。
        如果执行任务产生报错，请再次尝试。
        当你已能从当前工具结果中给出完整或部分答案时，请直接以纯文字回复用户（不要再次调用工具），总结地点、计划或说明无法完成的部分。
        若要在任何时候停止交互，请使用 doTerminate 工具调用，但调用前请确保已用文字回复过用户或说明原因，不要提前退出。
        """;

    public YuManus(ChatModel dashscopeChatModel, ToolCallback[] allTools) {
        super(dashscopeChatModel, allTools != null ? allTools : new ToolCallback[0]);
        setName("YuManus");
        setSystemPrompt(SYSTEM_PROMPT);
        setNextStepPrompt(NEXT_STEP_PROMPT);
        setMaxSteps(20);
    }
}

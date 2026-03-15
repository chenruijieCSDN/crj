package com.yupi.yuaiagent.agent;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ToolCallAgent 的配置：在存在 ChatModel 和 ToolCallback 数组时注册 Bean，便于手动控制工具调用流程。
 */
@Configuration
public class ToolCallAgentConfig {

    @Bean
    @ConditionalOnBean({ ChatModel.class })
    public ToolCallAgent toolCallAgent(ChatModel chatModel, ToolCallback[] allTools) {
        ToolCallAgent agent = new ToolCallAgent(chatModel, allTools != null ? allTools : new ToolCallback[0]);
        agent.setName("ToolCallAgent");
        agent.setSystemPrompt("你是一个有帮助的助手，可以根据用户需求调用可用工具完成任务。");
        return agent;
    }
}

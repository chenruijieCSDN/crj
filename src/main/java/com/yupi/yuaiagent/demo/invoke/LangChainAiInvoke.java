package com.yupi.yuaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
public class LangChainAiInvoke {

    public static void main(String[] args) {
        ChatLanguageModel qwenChatModel  = QwenChatModel.builder()
                .apiKey(TestApiKey.API_KEY)
                .modelName("qwen-plus")
                .build();
        String answer = qwenChatModel.chat("你好,我是小红");
        System.out.println(answer);
    }
}

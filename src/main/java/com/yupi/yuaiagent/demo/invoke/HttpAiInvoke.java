package com.yupi.yuaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

public class HttpAiInvoke {

    public static void main(String[] args) {
        String apiKey = TestApiKey.API_KEY;

        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 先构建 messages 数组（add 返回 boolean，不能链式）
        JSONArray messages = JSONUtil.createArray();
        messages.add(JSONUtil.createObj().set("role", "system").set("content", "You are a helpful assistant."));
        messages.add(JSONUtil.createObj().set("role", "user").set("content", "你是谁？"));

        String jsonBody = JSONUtil.createObj()
                .set("model", "qwen-plus")
                .set("input", JSONUtil.createObj().set("messages", messages))
                .set("parameters", JSONUtil.createObj().set("result_format", "message"))
                .toString();

        String response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .execute()
                .body();

        System.out.println(response);
    }
}
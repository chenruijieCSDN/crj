package com.yupi.yuaiagent.demo.invoke;

/**
 * Demo 用的 API Key，优先从环境变量 DASHSCOPE_API_KEY 读取，未设置时为空（需在 application-local 中配置）。
 */
public interface TestApiKey {

    String API_KEY = System.getenv("DASHSCOPE_API_KEY");
}

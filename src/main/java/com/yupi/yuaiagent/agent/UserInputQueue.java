package com.yupi.yuaiagent.agent;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 用户输入队列：用于 Manus 人机交互。
 * AI 通过 QueryUserTool 向用户提问时会阻塞在 takeResponse()，前端/接口调用 putResponse 后唤醒并返回用户输入。
 * 参考：<a href="https://www.codefather.cn/post/1928476155422310402">智能体Manus改进——人机交互</a>
 */
@Component
public class UserInputQueue {

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    public void putResponse(String response) throws InterruptedException {
        queue.put(response);
    }

    public String takeResponse() throws InterruptedException {
        return queue.take();
    }
}

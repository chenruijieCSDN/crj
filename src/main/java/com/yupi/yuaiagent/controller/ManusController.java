package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.UserInputQueue;
import com.yupi.yuaiagent.agent.YuManus;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manus 智能体相关接口：人机交互输入 + 发起 Manus 任务。
 * 参考：<a href="https://www.codefather.cn/post/1928476155422310402">智能体Manus改进——人机交互</a>
 */
@RestController
public class ManusController {

    @Resource
    private UserInputQueue userInputQueue;

    @Resource
    private YuManus yuManus;

    /**
     * 当 AI 通过 askUserAndWait 向用户提问时，前端调用此接口传入用户输入，唤醒阻塞并返回给模型。
     *
     * @param input 用户输入内容
     */
    @GetMapping("/user/input")
    public void userInput(@RequestParam("input") String input) {
        try {
            if (input == null || input.isBlank()) {
                userInputQueue.putResponse("用户输入为空");
            } else {
                userInputQueue.putResponse(input.trim());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用 YuManus 执行一次 ReAct 工具调用任务（含多轮 think-act，可配合 askUserAndWait 人机交互）。
     *
     * @param query 用户任务描述
     * @return 各步执行结果汇总，或最终文本
     */
    @GetMapping("/manus/run")
    public String manusRun(@RequestParam("query") String query) {
        if (query == null || query.isBlank()) {
            return "请输入任务描述（query）";
        }
        return yuManus.run(query);
    }
}

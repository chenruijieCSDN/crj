package com.yupi.yuaiagent.tools;

import com.yupi.yuaiagent.agent.UserInputQueue;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Manus 人机交互工具：向用户提问并阻塞等待用户输入，用户通过 /user/input 接口回传答案后返回。
 */
@Slf4j
@Component
public class QueryUserTool {

    @Resource
    private UserInputQueue userInputQueue;

    @Tool(description = "向用户提问并等待用户输入，用于在任务执行过程中向用户确认信息（如预算、日期、偏好等）。得到用户回答后继续执行。")
    public String askUserAndWait(@ToolParam(description = "向用户提出的问题") String question) {
        log.info("【系统提问】{}", question);
        try {
            String userAnswer = userInputQueue.takeResponse();
            log.info("【用户回答】{}", userAnswer);
            return userAnswer != null ? userAnswer : "用户未输入";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "用户取消了操作";
        }
    }
}

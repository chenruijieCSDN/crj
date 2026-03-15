package com.yupi.yuaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Manus 结束任务工具：模型在完成用户任务的最后一步后调用，表示任务结束。
 * 参考编程导航 Manus 教程中的 doTerminate。
 */
public class TerminateTool {

    @Tool(description = "在确认已完成用户要求的全部步骤后调用此工具，结束当前任务。不要在未完成任务时提前调用。")
    public String doTerminate(@ToolParam(description = "可选：结束原因或总结") String reason) {
        return "任务结束";
    }
}

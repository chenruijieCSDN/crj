package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TerminalOperationToolTest {

    @Test
    public void testExecuteTerminalCommand() {
        TerminalOperationTool tool = new TerminalOperationTool();
        // Windows 下使用 echo/dir；Linux/Mac 可用 ls。此处用跨平台易成功的命令
        String command = System.getProperty("os.name").toLowerCase().contains("windows") ? "echo hello" : "echo hello";
        String result = tool.executeTerminalCommand(command);
        assertNotNull(result);
        assertFalse(result.contains("Error executing command"), "终端命令应执行成功");
    }
}

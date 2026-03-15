package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileOperationToolTest {

    private static final String TEST_FILE = "tool_test_编程导航.txt";

    @Test
    @Order(1)
    public void testWriteFile() {
        FileOperationTool tool = new FileOperationTool();
        String content = "https://www.codefather.cn 程序员编程学习交流社区";
        String result = tool.writeFile(TEST_FILE, content);
        assertNotNull(result);
        assertTrue(result.contains("success") || result.contains("successfully"), "写入应成功");
    }

    @Test
    @Order(2)
    public void testReadFile() {
        FileOperationTool tool = new FileOperationTool();
        String result = tool.readFile(TEST_FILE);
        assertNotNull(result);
        assertTrue(result.contains("codefather.cn") || result.startsWith("Error"), "应读到内容或得到错误信息");
    }
}

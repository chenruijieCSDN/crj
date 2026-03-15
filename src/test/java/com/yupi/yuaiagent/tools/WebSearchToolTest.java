package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WebSearchToolTest {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Test
    public void testSearchWeb() {
        if (searchApiKey == null || searchApiKey.isBlank()) {
            // 未配置 API Key 时仅验证工具可构造
            assertNotNull(new WebSearchTool("dummy"));
            return;
        }
        WebSearchTool tool = new WebSearchTool(searchApiKey);
        String query = "程序员鱼皮编程导航 codefather.cn";
        String result = tool.searchWeb(query);
        assertNotNull(result);
        assertFalse(result.startsWith("Error searching Baidu"), "配置正确时搜索应成功");
    }
}

package com.yupi.yuaiagent.config;

import com.yupi.yuaiagent.tools.*;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * 测试用工具注册：与生产环境一致，但不包含 QueryUserTool，避免测试阻塞等待用户输入。
 * 与 liyupi/yu-ai-agent 原版测试行为一致。仅在 profile=test 时生效。
 */
@TestConfiguration
@Profile("test")
public class TestToolConfig {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Value("${amap.web-api-key:}")
    private String amapWebApiKey;

    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        AmapTool amapTool = new AmapTool(amapWebApiKey);
        TerminateTool terminateTool = new TerminateTool();
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                amapTool,
                terminateTool
        );
    }
}

package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.config.TestToolConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

/**
 * 与 liyupi/yu-ai-agent 原版测试一致。必须激活 profile=test，使用不包含 QueryUserTool 的工具集，否则会卡在「向用户提问」。
 * IDE 运行若卡住：在运行配置里添加 VM 选项 -Dspring.profiles.active=local,test
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@Import(TestToolConfig.class)
class YuManusTest {

    @Resource
    private YuManus yuManus;

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void run() {
        String userPrompt = """
                我的另一半居住在山西太原小店区，请帮我找到 15 公里内合适的约会地点，
                并结合一些网络图片，制定一份详细的约会计划，
                并以 PDF 格式输出""";
        String answer = yuManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}

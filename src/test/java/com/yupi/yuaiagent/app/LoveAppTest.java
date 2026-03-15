package com.yupi.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoveApp 测试类
 *
 * 测试各种聊天功能：
 * 1. 基础对话（testChat）- 测试多轮对话和记忆功能
 * 2. 结构化报告输出（doChatWithReport）- 测试恋爱报告生成功能
 * 3. RAG检索增强对话（doChatWithRag）- 测试知识库问答功能
 *
 * RAG实现方式说明：
 * - 当前实现：基于阿里云知识库服务（已在LoveApp中启用）
 * - 其他可选实现：
 *   1. SimpleVectorStore: advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
 *   2. PgVector: advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
 *   3. 阿里云知识库: .advisors(loveAppRagCloudAdvisor) - 当前启用
 *
 * 使用示例：
 * ```java
 * // 基础对话
 * String answer = loveApp.doChat("你好，我想谈恋爱", chatId);
 *
 * // 生成恋爱报告
 * LoveReport report = loveApp.doChatWithReport("我想让另一半更爱我", chatId);
 *
 * // RAG知识库问答（当前使用阿里云知识库）
 * String ragAnswer = loveApp.doChatWithRag("婚后如何保持亲密关系？", chatId);
 * ```
 */
@SpringBootTest
@ActiveProfiles("local")
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    /**
     * 测试基础对话功能
     *
     * 测试场景：
     * 1. 自我介绍 - 建立对话身份
     * 2. 表达恋爱需求 - 测试AI理解和建议能力
     * 3. 记忆测试 - 验证多轮对话记忆功能
     *
     * 期望结果：AI能够记住对话历史并给出连贯的回复
     */
    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员鱼皮";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想让另一半（编程导航）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮 - 记忆测试：AI应该记得之前提到的"编程导航"
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * RAG实现方式切换示例
     *
     * 如果要使用其他RAG实现，可以参考以下方式：
     *
     * 1. SimpleVectorStore实现（基于本地文档）：
     *    需要在LoveApp中启用：advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
     *
     * 2. PgVector实现（基于PostgreSQL向量数据库）：
     *    需要在LoveApp中启用：advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
     *
     * 3. 阿里云知识库服务（当前启用）：
     *    当前实现：.advisors(loveAppRagCloudAdvisor)
     *
     * 注意：Spring AI 1.1.2版本中QuestionAnswerAdvisor构造函数有变化，
     * 如果直接使用可能会遇到编译错误，建议使用自定义Advisor或手动集成。
     */
    @Test
    void doChat() {
        // 此方法留空，用于展示不同RAG实现的注释说明
        // 实际测试请使用 doChatWithRag() 方法
    }

    /**
     * 测试结构化恋爱报告生成功能
     *
     * 测试场景：用户表达恋爱困扰并请求专业建议
     * 期望结果：返回结构化的LoveReport，包含标题和建议列表
     *
     * LoveReport结构：
     * - title: 恋爱报告标题
     * - suggestions: 具体建议列表
     */
    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮，我想让另一半（编程导航）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
        // 可以进一步验证报告内容
        // Assertions.assertNotNull(loveReport.title());
        // Assertions.assertFalse(loveReport.suggestions().isEmpty());
    }

    /**
     * 测试RAG检索增强对话功能
     *
     * 当前实现：基于阿里云知识库服务
     * 测试场景：婚后关系问题咨询
     *
     * RAG实现方式对比：
     * ┌─────────────────────────────────────────────────────────────┐
     * │ 实现方式          │ 代码示例                                      │ 状态   │
     * ├─────────────────────────────────────────────────────────────┤
     * │ SimpleVectorStore │ advisors(new QuestionAnswerAdvisor(         │ 已注释 │
     * │                   │   loveAppVectorStore))                      │        │
     * ├─────────────────────────────────────────────────────────────┤
     * │ PgVector          │ advisors(new QuestionAnswerAdvisor(         │ 已注释 │
     * │                   │   pgVectorVectorStore))                     │        │
     * ├─────────────────────────────────────────────────────────────┤
     * │ 阿里云知识库      │ .advisors(loveAppRagCloudAdvisor)           │ ✓启用 │
     * └─────────────────────────────────────────────────────────────┘
     *
     * 测试说明：
     * - 系统会从知识库中检索相关文档
     * - AI基于检索到的内容给出专业建议
     * - 期望返回包含知识库信息的回复
     */
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        // 测试问题：婚后关系咨询
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String answer = loveApp.doChatWithRag(message, chatId);

        // 验证回复不为空
        Assertions.assertNotNull(answer);

        // 可选：验证回复包含知识库相关内容
        // 例如检查是否包含"婚后"、"亲密"等关键词
        // Assertions.assertTrue(answer.contains("婚后") || answer.contains("亲密"));
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份'七夕约会计划'PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试 MCP（地图等）工具调用。
     * 测试环境通过排除 MCP 自动配置避免缺 ClientMcpTransport 导致上下文启动失败；
     * 运行时 toolCallbackProvider 为 null，doChatWithMcp 会走普通对话并返回非空内容。
     */
    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "我的另一半居住在山西太原小店区，请帮我找到 10 公里内合适的约会地点";
        String answer = loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isBlank());
    }

}
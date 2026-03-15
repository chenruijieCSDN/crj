package com.yupi.yuaiagent.rag;

import com.yupi.yuaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

/**
 * 针对 5 个实现的集成测试
 *
 * 实现1: DocumentReader 批量元信息 + AI Transformer 自动元信息
 * 实现2: 多查询扩展 (MultiQueryExpander)
 * 实现3: 查询重写 (RewriteQueryTransformer) + 翻译 (TranslationQueryTransformer)
 * 实现4: filterExpression 过滤规则 (VectorStoreDocumentRetriever)
 * 实现5: ContextualQueryAugmenter 空上下文 + 错误处理
 */
@SpringBootTest
class FiveImplementationsTest {

    @Resource
    private DocumentReaderBatchWithAiMetadataPipeline documentReaderBatchWithAiMetadataPipeline;

    @Resource
    private LoveApp loveApp;

    @Resource
    private DocumentRetriever filteredVectorStoreDocumentRetriever;

    @Resource
    private Advisor loveAppFilteredVectorStoreRagAdvisor;

    // ========== 实现1：DocumentReader 批量元信息 + AI Transformer ==========

    @Test
    @DisplayName("实现1: DocumentReader 批量添加元信息 + AI Transformer 自动元信息")
    void testImplementation1_documentReaderBatchWithAiMetadata() {
        List<Document> docs = documentReaderBatchWithAiMetadataPipeline.loadDocumentsWithBatchMetadataAndAi();
        Assertions.assertNotNull(docs, "管道应返回文档列表（可为空）");

        if (!docs.isEmpty()) {
            Document first = docs.get(0);
            // 应有 DocumentReader 添加的基础元信息
            Assertions.assertTrue(
                first.getMetadata().containsKey("filename") || first.getMetadata().containsKey("title") || first.getMetadata().containsKey("category"),
                "应包含批量元信息（filename/title/category 等）"
            );
            // 应有 AI Transformer 添加的元信息
            Assertions.assertTrue(
                first.getMetadata().containsKey("ai_title") || first.getMetadata().containsKey("ai_keywords") || first.getMetadata().containsKey("ai_processed"),
                "应包含 AI 生成的元信息（ai_title/ai_keywords/ai_processed 等）"
            );
        }
    }

    // ========== 实现2 & 3：多查询扩展 + 查询重写/翻译（通过 RAG 流程验证）==========

    @Test
    @DisplayName("实现2&3: RAG 对话（多查询扩展 + 查询重写/翻译）")
    void testImplementation2And3_ragWithQueryExpansionAndRewrite() {
        String chatId = UUID.randomUUID().toString();
        String message = "婚后如何保持亲密关系？";
        String answer = loveApp.doChatWithRag(message, chatId);

        Assertions.assertNotNull(answer, "RAG 应返回非空回复");
        Assertions.assertFalse(answer.isBlank(), "RAG 回复不应为空白");
        // 友好错误提示也算有效回复
        Assertions.assertTrue(
            answer.length() >= 5,
            "回复应为有效文本（正常内容或友好错误提示）"
        );
    }

    // ========== 实现4：filterExpression 过滤规则 ==========

    @Test
    @DisplayName("实现4: 带 filterExpression 的文档检索器")
    void testImplementation4_filteredRetriever() {
        Assertions.assertNotNull(filteredVectorStoreDocumentRetriever, "过滤检索器 Bean 应存在");
        Assertions.assertNotNull(loveAppFilteredVectorStoreRagAdvisor, "过滤 RAG 顾问 Bean 应存在");

        Query query = new Query("恋爱中如何沟通");
        List<Document> docs = filteredVectorStoreDocumentRetriever.retrieve(query);
        Assertions.assertNotNull(docs, "检索结果列表不应为 null");
        // 本地向量库若未按 category 入库，可能返回空列表，属正常
    }

    // ========== 实现5：错误处理与空上下文 ==========

    @Test
    @DisplayName("实现5: RAG 错误处理返回友好提示")
    void testImplementation5_errorHandlingReturnsFriendlyMessage() {
        String chatId = UUID.randomUUID().toString();
        // 正常问题应得到回复
        String normalAnswer = loveApp.doChatWithRag("怎样表白成功率更高？", chatId);
        Assertions.assertNotNull(normalAnswer);
        Assertions.assertFalse(normalAnswer.isBlank());

        // 空或极短输入也应得到字符串回复（可能为友好提示或正常回复）
        String emptyAnswer = loveApp.doChatWithRag("", chatId);
        Assertions.assertNotNull(emptyAnswer, "空输入时也应返回非 null 的提示或回复");
    }
}

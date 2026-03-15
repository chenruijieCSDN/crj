package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强文档处理功能测试类
 */
@SpringBootTest
public class EnhancedDocumentProcessingTest {

    @Resource
    private ResourcePatternResolver resourcePatternResolver;

    @Resource
    private AiMetadataTransformer aiMetadataTransformer;

    @Resource
    private DocumentProcessingConfig.LoveAppDocumentProcessor documentProcessor;

    @Test
    public void testEnhancedDocumentLoader() {
        // 创建增强的文档加载器
        EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(resourcePatternResolver);

        // 为每个文件配置特定的元信息
        Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> fileMetadataMap = new HashMap<>();

        // 配置"恋爱篇"文档的元信息
        EnhancedLoveAppDocumentLoader.DocumentMetadata loveMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
        loveMetadata.setTitle("恋爱技巧完全指南");
        loveMetadata.setAuthor("恋爱专家团队");
        loveMetadata.setCategory("恋爱技巧");
        loveMetadata.setTags(List.of("恋爱", "约会", "表白", "相处"));
        loveMetadata.setDescription("从相识到相恋的完整指导");
        fileMetadataMap.put("恋爱常见问题和回答 - 恋爱篇.md", loveMetadata);

        // 配置"单身篇"文档的元信息
        EnhancedLoveAppDocumentLoader.DocumentMetadata singleMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
        singleMetadata.setTitle("单身生活指南");
        singleMetadata.setAuthor("情感顾问");
        singleMetadata.setCategory("单身指导");
        singleMetadata.setTags(List.of("单身", "自我提升", "社交", "脱单"));
        singleMetadata.setDescription("如何享受单身生活并为恋爱做准备");
        fileMetadataMap.put("恋爱常见问题和回答 - 单身篇.md", singleMetadata);

        // 配置"已婚篇"文档的元信息
        EnhancedLoveAppDocumentLoader.DocumentMetadata marriedMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
        marriedMetadata.setTitle("婚姻经营之道");
        marriedMetadata.setAuthor("婚姻家庭咨询师");
        marriedMetadata.setCategory("婚姻指导");
        marriedMetadata.setTags(List.of("婚姻", "夫妻", "沟通", "家庭"));
        marriedMetadata.setDescription("维持幸福婚姻的实用建议");
        fileMetadataMap.put("恋爱常见问题和回答 - 已婚篇.md", marriedMetadata);

        // 批量加载文档并添加元信息
        List<Document> documents = loader.loadMarkdownsWithMetadata(fileMetadataMap);

        // 打印处理结果
        System.out.println("=== 批量元信息处理结果 ===");
        System.out.println("处理文档数量: " + documents.size());

        // 显示每个文档的元信息
        for (int i = 0; i < Math.min(3, documents.size()); i++) {
            Document doc = documents.get(i);
            System.out.println("\n文档 " + (i + 1) + " 元信息:");
            doc.getMetadata().forEach((key, value) -> {
                System.out.println("  " + key + ": " + value);
            });
        }

        // 显示文档摘要
        Map<String, Object> summary = loader.getDocumentSummary(documents);
        System.out.println("\n=== 文档摘要统计 ===");
        summary.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    @Test
    public void testAiMetadataTransformer() {
        // 首先加载基础文档
        EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(resourcePatternResolver);
        EnhancedLoveAppDocumentLoader.DocumentMetadata basicMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
        basicMetadata.setTitle("恋爱指导文档");
        basicMetadata.setAuthor("系统");
        basicMetadata.setCategory("恋爱指导");

        List<Document> documents = loader.loadMarkdownsWithBatchMetadata(basicMetadata);

        // 使用AI自动生成元信息
        List<Document> aiProcessedDocs = aiMetadataTransformer.apply(documents);

        // 显示AI处理结果
        System.out.println("\n=== AI元信息处理结果 ===");
        System.out.println("AI处理文档数量: " + aiProcessedDocs.size());

        // 显示前3个文档的AI元信息
        for (int i = 0; i < Math.min(3, aiProcessedDocs.size()); i++) {
            Document doc = aiProcessedDocs.get(i);
            System.out.println("\n文档 " + (i + 1) + " AI元信息:");
            Map<String, Object> metadata = doc.getMetadata();
            metadata.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("ai_"))
                    .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        }

        // 显示处理统计
        Map<String, Object> stats = aiMetadataTransformer.getProcessingStats(aiProcessedDocs);
        System.out.println("\n=== AI处理统计 ===");
        stats.forEach((key, value) -> System.out.println(key + ": " + value));

        // 显示热门关键词
        System.out.println("\n=== 热门关键词TOP5 ===");
        aiMetadataTransformer.getPopularKeywords(aiProcessedDocs, 5)
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue() + "次"));
    }

    @Test
    public void testCombinedProcessing() {
        // 创建特定的元信息配置
        Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> fileMetadataMap = new HashMap<>();

        // 为恋爱篇配置更详细的元信息
        EnhancedLoveAppDocumentLoader.DocumentMetadata loveMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
        loveMetadata.setTitle("恋爱完全手册");
        loveMetadata.setAuthor("资深情感专家");
        loveMetadata.setCategory("恋爱技巧");
        loveMetadata.setTags(List.of("恋爱", "技巧", "约会", "表白", "相处"));
        loveMetadata.setDescription("从初识到确立关系的完整指导");

        // 添加自定义字段
        Map<String, Object> customFields = Map.of(
            "difficulty", "中级",
            "targetAudience", "18-35岁单身人群",
            "contentType", "问答指导",
            "estimatedReadingTime", "15分钟"
        );
        loveMetadata.setAdditionalMetadata(customFields);
        fileMetadataMap.put("恋爱常见问题和回答 - 恋爱篇.md", loveMetadata);

        // 使用综合处理器
        EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(resourcePatternResolver);
        List<Document> documents = loader.loadMarkdownsWithMetadata(fileMetadataMap);
        List<Document> processedDocs = documentProcessor.processWithAiOnly(documents);

        // 显示最终结果
        System.out.println("\n=== 综合处理结果 ===");
        System.out.println("总处理文档数: " + processedDocs.size());

        // 显示融合后的元信息
        if (!processedDocs.isEmpty()) {
            Document firstDoc = processedDocs.get(0);
            System.out.println("\n融合后的元信息示例:");
            firstDoc.getMetadata().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        }
    }
}
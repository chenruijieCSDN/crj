package com.yupi.yuaiagent.rag;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 基于AI的元信息生成Transformer
 * 使用Spring AI的ChatClient自动解析文档内容并生成元信息
 */
@Component
@Slf4j
public class AiMetadataTransformer implements DocumentTransformer {

    private final ChatClient chatClient;

    @Autowired
    public AiMetadataTransformer(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * AI生成的元信息结果
     */
    @Data
    public static class AiGeneratedMetadata {
        private String title;
        private String summary;
        private List<String> keywords;
        private String category;
        private String sentiment;
        private List<String> topics;
        private Map<String, Object> customFields;

        public AiGeneratedMetadata() {
            this.keywords = new ArrayList<>();
            this.topics = new ArrayList<>();
            this.customFields = new HashMap<>();
        }
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return documents;
        }

        log.info("开始AI元信息转换，文档数量: {}", documents.size());
        List<Document> transformedDocs = new ArrayList<>();

        // 批量处理文档，每个文档异步处理
        List<CompletableFuture<Document>> futures = documents.stream()
                .map(doc -> CompletableFuture.supplyAsync(() -> processDocumentWithAi(doc)))
                .collect(Collectors.toList());

        // 等待所有文档处理完成
        for (CompletableFuture<Document> future : futures) {
            try {
                transformedDocs.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("AI元信息处理失败", e);
            }
        }

        log.info("AI元信息转换完成");
        return transformedDocs;
    }

    /**
     * 使用AI处理单个文档
     */
    private Document processDocumentWithAi(Document document) {
        try {
            String content = document.getText();

            // 如果内容过长，截取前2000个字符进行分析
            if (content.length() > 2000) {
                content = content.substring(0, 2000) + "...";
            }

            // 构建AI提示词
            String prompt = buildMetadataExtractionPrompt(content);

            // 调用AI生成元信息
            AiGeneratedMetadata aiMetadata = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(AiGeneratedMetadata.class);

            // 将AI生成的元信息添加到文档中
            addAiGeneratedMetadata(document, aiMetadata);

            log.debug("AI成功为文档生成元信息: title={}, keywords={}",
                    aiMetadata.getTitle(), aiMetadata.getKeywords());

        } catch (Exception e) {
            log.error("AI元信息生成失败，将使用默认元信息", e);
            addDefaultMetadata(document);
        }

        return document;
    }

    /**
     * 构建元信息提取提示词
     */
    private String buildMetadataExtractionPrompt(String content) {
        return """
                请分析以下文档内容，并生成结构化的元信息：

                文档内容：
                """ + content + """

                请按照以下JSON格式返回分析结果：
                {
                    "title": "文档标题（简洁明了）",
                    "summary": "文档摘要（50-100字）",
                    "keywords": ["关键词1", "关键词2", "关键词3"],
                    "category": "文档分类（如：恋爱技巧、婚姻指导、情感挽回等）",
                    "sentiment": "情感倾向（积极/中性/消极）",
                    "topics": ["主题1", "主题2"],
                    "customFields": {
                        "difficulty": "难度等级（入门/进阶/高级）",
                        "applicableStage": "适用阶段（单身/恋爱中/已婚）",
                        "contentType": "内容类型（问答/指导/案例分析）"
                    }
                }

                要求：
                1. 关键词应准确反映文档核心内容
                2. 分类要符合恋爱指导领域的常见分类
                3. 主题要具体明确
                4. 自定义字段要根据内容实际情况填写
                """;
    }

    /**
     * 将AI生成的元信息添加到文档
     */
    private void addAiGeneratedMetadata(Document document, AiGeneratedMetadata aiMetadata) {
        Map<String, Object> metadata = document.getMetadata();

        // 基础元信息
        metadata.put("ai_title", aiMetadata.getTitle());
        metadata.put("ai_summary", aiMetadata.getSummary());
        metadata.put("ai_keywords", aiMetadata.getKeywords());
        metadata.put("ai_category", aiMetadata.getCategory());
        metadata.put("ai_sentiment", aiMetadata.getSentiment());
        metadata.put("ai_topics", aiMetadata.getTopics());

        // 自定义字段
        metadata.putAll(aiMetadata.getCustomFields());

        // 添加处理标记
        metadata.put("ai_processed", true);
        metadata.put("ai_processed_time", System.currentTimeMillis());
        metadata.put("ai_processed_date", new Date().toString());

        // 计算内容质量分数
        double qualityScore = calculateContentQuality(document.getText(), aiMetadata);
        metadata.put("ai_quality_score", qualityScore);
    }

    /**
     * 添加默认元信息（当AI处理失败时）
     */
    private void addDefaultMetadata(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        metadata.put("ai_processed", false);
        metadata.put("ai_error", true);
        metadata.put("ai_error_time", new Date().toString());
        metadata.put("ai_title", "未处理");
        metadata.put("ai_summary", "AI处理失败，使用默认元信息");
        metadata.put("ai_keywords", Arrays.asList("恋爱", "指导"));
        metadata.put("ai_category", "其他");
        metadata.put("ai_sentiment", "未知");
        metadata.put("ai_topics", Arrays.asList("恋爱指导"));
        metadata.put("ai_quality_score", 0.5);
    }

    /**
     * 计算内容质量分数
     */
    private double calculateContentQuality(String content, AiGeneratedMetadata aiMetadata) {
        double score = 0.5; // 基础分数

        // 根据内容长度调整分数
        if (content.length() > 100) {
            score += 0.1;
        }
        if (content.length() > 500) {
            score += 0.1;
        }

        // 根据关键词数量调整分数
        if (aiMetadata.getKeywords() != null) {
            if (aiMetadata.getKeywords().size() >= 3) {
                score += 0.1;
            }
            if (aiMetadata.getKeywords().size() >= 5) {
                score += 0.1;
            }
        }

        // 根据主题数量调整分数
        if (aiMetadata.getTopics() != null && aiMetadata.getTopics().size() > 0) {
            score += 0.1;
        }

        // 确保分数在0-1之间
        return Math.min(1.0, score);
    }

    /**
     * 批量处理文档，支持异步处理
     */
    public CompletableFuture<List<Document>> applyAsync(List<Document> documents) {
        return CompletableFuture.supplyAsync(() -> apply(documents));
    }

    /**
     * 获取AI元信息处理的统计信息
     */
    public Map<String, Object> getProcessingStats(List<Document> documents) {
        Map<String, Object> stats = new HashMap<>();

        long totalDocs = documents.size();
        long processedDocs = documents.stream()
                .filter(doc -> Boolean.TRUE.equals(doc.getMetadata().get("ai_processed")))
                .count();
        long errorDocs = documents.stream()
                .filter(doc -> Boolean.TRUE.equals(doc.getMetadata().get("ai_error")))
                .count();

        stats.put("totalDocuments", totalDocs);
        stats.put("processedDocuments", processedDocs);
        stats.put("errorDocuments", errorDocs);
        stats.put("successRate", totalDocs > 0 ? (double) processedDocs / totalDocs : 0.0);

        // 分类统计
        Map<String, Long> categoryStats = documents.stream()
                .filter(doc -> doc.getMetadata().containsKey("ai_category"))
                .collect(Collectors.groupingBy(
                        doc -> (String) doc.getMetadata().get("ai_category"),
                        Collectors.counting()
                ));
        stats.put("categoryDistribution", categoryStats);

        // 情感倾向统计
        Map<String, Long> sentimentStats = documents.stream()
                .filter(doc -> doc.getMetadata().containsKey("ai_sentiment"))
                .collect(Collectors.groupingBy(
                        doc -> (String) doc.getMetadata().get("ai_sentiment"),
                        Collectors.counting()
                ));
        stats.put("sentimentDistribution", sentimentStats);

        // 平均质量分数
        double avgQualityScore = documents.stream()
                .filter(doc -> doc.getMetadata().containsKey("ai_quality_score"))
                .mapToDouble(doc -> (Double) doc.getMetadata().get("ai_quality_score"))
                .average()
                .orElse(0.0);
        stats.put("averageQualityScore", avgQualityScore);

        return stats;
    }

    /**
     * 获取热门关键词
     */
    public List<Map.Entry<String, Long>> getPopularKeywords(List<Document> documents, int topN) {
        Map<String, Long> keywordCount = new HashMap<>();

        documents.forEach(doc -> {
            Object keywordsObj = doc.getMetadata().get("ai_keywords");
            if (keywordsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) keywordsObj;
                keywords.forEach(keyword ->
                    keywordCount.put(keyword, keywordCount.getOrDefault(keyword, 0L) + 1)
                );
            }
        });

        return keywordCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }
}
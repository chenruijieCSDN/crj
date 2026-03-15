package com.yupi.yuaiagent.rag;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * 增强的恋爱大师应用文档加载器，支持批量添加元信息
 */
@Component
@Slf4j
public class EnhancedLoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    EnhancedLoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 文档元信息配置类
     */
    @Data
    public static class DocumentMetadata {
        private String title;
        private String author;
        private String category;
        private List<String> tags;
        private String description;
        private Map<String, Object> additionalMetadata;

        public DocumentMetadata() {
            this.tags = new ArrayList<>();
            this.additionalMetadata = new HashMap<>();
        }
    }

    /**
     * 批量加载Markdown文档并添加元信息
     */
    public List<Document> loadMarkdownsWithMetadata(Map<String, DocumentMetadata> fileMetadataMap) {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");

            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                log.info("正在处理文档: {}", fileName);

                // 获取或创建元信息配置
                DocumentMetadata metadata = fileMetadataMap.getOrDefault(fileName, createDefaultMetadata(fileName));

                // 构建Markdown文档读取器配置
                MarkdownDocumentReaderConfig config = buildMetadataConfig(fileName, metadata);

                // 读取文档
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                List<Document> documents = reader.get();

                // 为每个文档片段添加元信息
                for (Document doc : documents) {
                    addEnhancedMetadata(doc, metadata, fileName);
                }

                allDocuments.addAll(documents);
                log.info("成功处理文档: {}，生成 {} 个文档片段", fileName, documents.size());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }

    /**
     * 简化版：为所有文档批量添加相同的元信息
     */
    public List<Document> loadMarkdownsWithBatchMetadata(DocumentMetadata metadata) {
        Map<String, DocumentMetadata> batchMetadata = new HashMap<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                batchMetadata.put(resource.getFilename(), metadata);
            }
        } catch (IOException e) {
            log.error("获取文档列表失败", e);
        }
        return loadMarkdownsWithMetadata(batchMetadata);
    }

    /**
     * 根据文件名自动生成基础元信息
     */
    private DocumentMetadata createDefaultMetadata(String fileName) {
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setTitle(extractTitleFromFilename(fileName));
        metadata.setAuthor("恋爱大师团队");
        metadata.setCategory(determineCategory(fileName));
        metadata.setTags(generateDefaultTags(fileName));
        metadata.setDescription("关于" + metadata.getTitle() + "的恋爱指导内容");
        return metadata;
    }

    /**
     * 从文件名提取标题
     */
    private String extractTitleFromFilename(String fileName) {
        // 移除文件扩展名和路径
        String title = fileName.replace(".md", "");
        // 移除前缀
        if (title.contains(" - ")) {
            title = title.substring(title.lastIndexOf(" - ") + 3);
        }
        return title;
    }

    /**
     * 根据文件名确定分类
     */
    private String determineCategory(String fileName) {
        if (fileName.contains("单身")) {
            return "单身指南";
        } else if (fileName.contains("恋爱")) {
            return "恋爱技巧";
        } else if (fileName.contains("已婚")) {
            return "婚姻生活";
        }
        return "恋爱指导";
    }

    /**
     * 生成默认标签
     */
    private List<String> generateDefaultTags(String fileName) {
        List<String> tags = new ArrayList<>();
        if (fileName.contains("单身")) {
            tags.addAll(Arrays.asList("单身", "脱单", "自我提升"));
        } else if (fileName.contains("恋爱")) {
            tags.addAll(Arrays.asList("恋爱", "约会", "表白", "相处"));
        } else if (fileName.contains("已婚")) {
            tags.addAll(Arrays.asList("婚姻", "夫妻", "家庭", "沟通"));
        }
        tags.add("恋爱指导");
        return tags;
    }

    /**
     * 构建Markdown文档读取器配置
     */
    private MarkdownDocumentReaderConfig buildMetadataConfig(String fileName, DocumentMetadata metadata) {
        MarkdownDocumentReaderConfig.Builder builder = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false);

        // 添加基础元信息
        builder.withAdditionalMetadata("filename", fileName);
        builder.withAdditionalMetadata("title", metadata.getTitle());
        builder.withAdditionalMetadata("author", metadata.getAuthor());
        builder.withAdditionalMetadata("category", metadata.getCategory());
        builder.withAdditionalMetadata("tags", String.join(",", metadata.getTags()));
        builder.withAdditionalMetadata("description", metadata.getDescription());
        builder.withAdditionalMetadata("createTime", new Date().toString());

        // 添加额外元信息
        metadata.getAdditionalMetadata().forEach(builder::withAdditionalMetadata);

        return builder.build();
    }

    /**
     * 为文档添加增强元信息
     */
    private void addEnhancedMetadata(Document document, DocumentMetadata metadata, String fileName) {
        Map<String, Object> docMetadata = document.getMetadata();

        // 确保所有基础元信息都已添加
        docMetadata.putIfAbsent("filename", fileName);
        docMetadata.putIfAbsent("title", metadata.getTitle());
        docMetadata.putIfAbsent("author", metadata.getAuthor());
        docMetadata.putIfAbsent("category", metadata.getCategory());
        docMetadata.putIfAbsent("tags", metadata.getTags());
        docMetadata.putIfAbsent("description", metadata.getDescription());

        // 添加文档统计信息
        docMetadata.put("contentLength", document.getText().length());
        docMetadata.put("wordCount", document.getText().split("\\s+").length);

        // 添加时间戳
        docMetadata.put("processedTime", System.currentTimeMillis());
        docMetadata.put("processedDate", new Date().toString());
    }

    /**
     * 获取文档元信息摘要
     */
    public Map<String, Object> getDocumentSummary(List<Document> documents) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDocuments", documents.size());

        // 按分类统计
        Map<String, Long> categoryCount = new HashMap<>();
        // 按作者统计
        Map<String, Long> authorCount = new HashMap<>();
        // 标签统计
        Map<String, Long> tagCount = new HashMap<>();

        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();

            // 分类统计
            String category = (String) metadata.getOrDefault("category", "未知");
            categoryCount.put(category, categoryCount.getOrDefault(category, 0L) + 1);

            // 作者统计
            String author = (String) metadata.getOrDefault("author", "未知");
            authorCount.put(author, authorCount.getOrDefault(author, 0L) + 1);

            // 标签统计
            Object tagsObj = metadata.get("tags");
            if (tagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) tagsObj;
                for (String tag : tags) {
                    tagCount.put(tag, tagCount.getOrDefault(tag, 0L) + 1);
                }
            }
        }

        summary.put("categoryCount", categoryCount);
        summary.put("authorCount", authorCount);
        summary.put("tagCount", tagCount);

        return summary;
    }
}
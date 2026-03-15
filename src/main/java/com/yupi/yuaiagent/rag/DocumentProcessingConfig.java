package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文档处理配置类
 * 整合批量添加元信息和AI自动添加元信息功能
 *
 * 实现1：利用 DocumentReader 批量添加元信息 + AI Transformer 自动生成元信息
 * - 使用 MarkdownDocumentReader（DocumentReader）按文件批量读取并添加元信息
 * - 使用 AiMetadataTransformer（Spring AI 风格 Transformer）基于 AI 自动解析关键词等并写入元信息
 */
@Configuration
@Slf4j
public class DocumentProcessingConfig {

    @Autowired
    private AiMetadataTransformer aiMetadataTransformer;

    @Autowired(required = false)
    private EnhancedLoveAppDocumentLoader enhancedLoveAppDocumentLoader;

    /**
     * 实现1：使用 DocumentReader 批量添加元信息，再通过 AI Transformer 自动生成关键词等元信息
     * 供向量库重建或文档处理 API 使用
     */
    @Bean
    public DocumentReaderBatchWithAiMetadataPipeline documentReaderBatchWithAiMetadataPipeline() {
        return new DocumentReaderBatchWithAiMetadataPipeline(aiMetadataTransformer, enhancedLoveAppDocumentLoader);
    }

    @Bean
    @Primary
    public LoveAppDocumentProcessor loveAppDocumentProcessor() {
        return new LoveAppDocumentProcessor();
    }

    /**
     * 文档处理器类
     */
    public class LoveAppDocumentProcessor {

        private final AiMetadataTransformer aiMetadataTransformer;

        public LoveAppDocumentProcessor() {
            this.aiMetadataTransformer = DocumentProcessingConfig.this.aiMetadataTransformer;
        }

        /**
         * 处理文档：先批量添加基础元信息，再通过AI增强
         */
        public List<Document> processDocuments(List<Document> documents,
                                               Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> fileMetadataMap) {
            log.info("开始文档处理管道...");

            // 第一步：批量添加基础元信息
            log.info("步骤1：批量添加基础元信息");
            EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(null);
            List<Document> docsWithBasicMetadata = loader.loadMarkdownsWithMetadata(fileMetadataMap);

            // 第二步：AI自动生成元信息
            log.info("步骤2：AI自动生成元信息");
            List<Document> docsWithAiMetadata = aiMetadataTransformer.apply(docsWithBasicMetadata);

            log.info("文档处理管道完成，共处理 {} 个文档", docsWithAiMetadata.size());
            return docsWithAiMetadata;
        }

        /**
         * 简化的处理方法：统一添加基础元信息
         */
        public List<Document> processWithBatchMetadata(List<Document> documents,
                                                       EnhancedLoveAppDocumentLoader.DocumentMetadata metadata) {
            EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(null);
            List<Document> docsWithBasicMetadata = loader.loadMarkdownsWithBatchMetadata(metadata);
            return aiMetadataTransformer.apply(docsWithBasicMetadata);
        }

        /**
         * 仅使用AI处理文档
         */
        public List<Document> processWithAiOnly(List<Document> documents) {
            return aiMetadataTransformer.apply(documents);
        }

        /**
         * 异步处理文档
         */
        public CompletableFuture<List<Document>> processDocumentsAsync(List<Document> documents,
                                                                       Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> fileMetadataMap) {
            return CompletableFuture.supplyAsync(() -> processDocuments(documents, fileMetadataMap));
        }
    }

    /**
     * 创建用于处理恋爱文档的预配置Transformer
     */
    @Bean
    public DocumentTransformer loveAppMetadataTransformer() {
        return new DocumentTransformer() {
            @Override
            public List<Document> apply(List<Document> documents) {
                // 创建恋爱应用专用的元信息配置
                EnhancedLoveAppDocumentLoader.DocumentMetadata loveAppMetadata =
                    new EnhancedLoveAppDocumentLoader.DocumentMetadata();
                loveAppMetadata.setTitle("恋爱指导文档");
                loveAppMetadata.setAuthor("恋爱大师AI");
                loveAppMetadata.setCategory("恋爱指导");
                loveAppMetadata.setTags(List.of("恋爱", "情感", "指导"));
                loveAppMetadata.setDescription("专业的恋爱指导内容");

                // 添加自定义字段
                Map<String, Object> customFields = Map.of(
                    "app", "恋爱大师",
                    "version", "1.0",
                    "language", "中文",
                    "targetAudience", "单身及恋爱中人群"
                );
                loveAppMetadata.setAdditionalMetadata(customFields);

                // 使用处理器处理文档
                LoveAppDocumentProcessor processor = new LoveAppDocumentProcessor();
                return processor.processWithBatchMetadata(documents, loveAppMetadata);
            }
        };
    }

    /**
     * 创建自定义Transformer，允许用户自定义元信息
     */
    @Bean
    public CustomMetadataTransformer customMetadataTransformer() {
        return new CustomMetadataTransformer(aiMetadataTransformer);
    }

    /**
     * 自定义元信息Transformer
     */
    public static class CustomMetadataTransformer implements DocumentTransformer {
        private final AiMetadataTransformer aiMetadataTransformer;
        private EnhancedLoveAppDocumentLoader.DocumentMetadata customMetadata;
        private boolean useAi = true;

        public CustomMetadataTransformer(AiMetadataTransformer aiMetadataTransformer) {
            this.aiMetadataTransformer = aiMetadataTransformer;
        }

        public CustomMetadataTransformer withMetadata(EnhancedLoveAppDocumentLoader.DocumentMetadata metadata) {
            this.customMetadata = metadata;
            return this;
        }

        public CustomMetadataTransformer withAi(boolean useAi) {
            this.useAi = useAi;
            return this;
        }

        @Override
        public List<Document> apply(List<Document> documents) {
            if (documents == null || documents.isEmpty()) {
                return documents;
            }

            List<Document> processedDocs = documents;

            // 如果指定了自定义元信息，先添加基础元信息
            if (customMetadata != null) {
                EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(null);
                // 这里简化处理，实际使用时应该根据文档来源分别处理
                processedDocs = loader.loadMarkdownsWithBatchMetadata(customMetadata);
            }

            // 如果使用AI增强，再进行AI处理
            if (useAi) {
                processedDocs = aiMetadataTransformer.apply(processedDocs);
            }

            return processedDocs;
        }
    }
}
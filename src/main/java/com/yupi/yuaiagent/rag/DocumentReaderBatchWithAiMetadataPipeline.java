package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现1：利用 DocumentReader 批量添加元信息 + AI Transformer 自动生成元信息
 * <ul>
 *   <li>使用 DocumentReader（MarkdownDocumentReader）批量读取文档并添加元信息</li>
 *   <li>使用 Spring AI 风格的 Transformer（AiMetadataTransformer）基于 AI 自动解析关键词等并写入元信息</li>
 * </ul>
 */
@Slf4j
public class DocumentReaderBatchWithAiMetadataPipeline {

    private final AiMetadataTransformer aiMetadataTransformer;
    private final EnhancedLoveAppDocumentLoader enhancedLoveAppDocumentLoader;

    public DocumentReaderBatchWithAiMetadataPipeline(AiMetadataTransformer aiMetadataTransformer,
                                                     EnhancedLoveAppDocumentLoader enhancedLoveAppDocumentLoader) {
        this.aiMetadataTransformer = aiMetadataTransformer;
        this.enhancedLoveAppDocumentLoader = enhancedLoveAppDocumentLoader;
    }

    /**
     * 先通过 DocumentReader 批量加载并添加元信息，再通过 AI Transformer 自动生成关键词等元信息
     */
    public List<Document> loadDocumentsWithBatchMetadataAndAi() {
        if (enhancedLoveAppDocumentLoader == null) {
            log.warn("EnhancedLoveAppDocumentLoader 未注入，仅返回空列表");
            return new ArrayList<>();
        }
        EnhancedLoveAppDocumentLoader.DocumentMetadata batchMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
        batchMetadata.setTitle("恋爱指导文档");
        batchMetadata.setAuthor("恋爱大师团队");
        batchMetadata.setCategory("恋爱指导");
        batchMetadata.setTags(List.of("恋爱", "情感", "指导"));
        batchMetadata.setDescription("基于知识库的恋爱指导内容");
        List<Document> withBasicMetadata = enhancedLoveAppDocumentLoader.loadMarkdownsWithBatchMetadata(batchMetadata);
        return aiMetadataTransformer.apply(withBasicMetadata);
    }
}

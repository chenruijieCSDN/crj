package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.rag.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文档处理控制器
 * 提供文档元信息处理的REST API接口
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档处理", description = "文档元信息处理相关接口")
@Slf4j
public class DocumentProcessingController {

    @Autowired
    private ApplicationContext applicationContext;

    private DocumentProcessingConfig.LoveAppDocumentProcessor documentProcessor;

    @Autowired
    private AiMetadataTransformer aiMetadataTransformer;

    @Autowired
    private EnhancedLoveAppDocumentLoader enhancedDocumentLoader;

    /**
     * 批量添加元信息接口
     */
    @PostMapping("/process/batch-metadata")
    @Operation(summary = "批量添加元信息", description = "为文档批量添加基础元信息")
    public ResponseEntity<? extends Map<String, Object>> addBatchMetadata(@RequestBody BatchMetadataRequest request) {
        try {
            // 构建文档元信息映射
            Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> metadataMap = new HashMap<>();

            request.getFileMetadata().forEach((filename, meta) -> {
                EnhancedLoveAppDocumentLoader.DocumentMetadata metadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
                metadata.setTitle(meta.getTitle());
                metadata.setAuthor(meta.getAuthor());
                metadata.setCategory(meta.getCategory());
                metadata.setTags(meta.getTags());
                metadata.setDescription(meta.getDescription());
                metadata.setAdditionalMetadata(meta.getAdditionalFields());
                metadataMap.put(filename, metadata);
            });

            // 处理文档
            List<Document> processedDocs = enhancedDocumentLoader.loadMarkdownsWithMetadata(metadataMap);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processedCount", processedDocs.size());
            response.put("summary", enhancedDocumentLoader.getDocumentSummary(processedDocs));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量添加元信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * AI自动生成元信息接口
     */
    @PostMapping("/process/ai-metadata")
    @Operation(summary = "AI自动生成元信息", description = "使用AI自动分析文档内容并生成元信息")
    public ResponseEntity<? extends Map<String, Object>> generateAiMetadata(@RequestBody List<Document> documents) {
        try {
            // 使用AI生成元信息
            List<Document> processedDocs = aiMetadataTransformer.apply(documents);

            // 获取统计信息
            Map<String, Object> stats = aiMetadataTransformer.getProcessingStats(processedDocs);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processedDocuments", processedDocs);
            response.put("statistics", stats);
            response.put("popularKeywords", aiMetadataTransformer.getPopularKeywords(processedDocs, 10));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("AI生成元信息失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 综合处理接口
     */
    @PostConstruct
    public void init() {
        this.documentProcessor = applicationContext.getBean(DocumentProcessingConfig.LoveAppDocumentProcessor.class);
    }

    @PostMapping("/process/comprehensive")
    @Operation(summary = "综合处理文档", description = "先批量添加基础元信息，再通过AI增强")
    public ResponseEntity<? extends Map<String, Object>> comprehensiveProcess(@RequestBody ComprehensiveProcessRequest request) {
        try {
            // 构建文档元信息映射
            Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> metadataMap = new HashMap<>();

            if (request.getFileMetadata() != null) {
                request.getFileMetadata().forEach((filename, meta) -> {
                    EnhancedLoveAppDocumentLoader.DocumentMetadata metadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
                    metadata.setTitle(meta.getTitle());
                    metadata.setAuthor(meta.getAuthor());
                    metadata.setCategory(meta.getCategory());
                    metadata.setTags(meta.getTags());
                    metadata.setDescription(meta.getDescription());
                    metadata.setAdditionalMetadata(meta.getAdditionalFields());
                    metadataMap.put(filename, metadata);
                });
            }

            // 异步处理文档
            CompletableFuture<List<Document>> future = documentProcessor.processDocumentsAsync(
                request.getDocuments(), metadataMap);

            // 等待处理完成
            List<Document> processedDocs = future.get();

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("processedCount", processedDocs.size());
            response.put("sampleMetadata", processedDocs.isEmpty() ? null : processedDocs.get(0).getMetadata());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("综合处理文档失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 获取处理统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取处理统计", description = "获取文档处理的统计信息")
    public ResponseEntity<Map<String, Object>> getProcessingStats(@RequestParam List<Document> documents) {
        Map<String, Object> stats = aiMetadataTransformer.getProcessingStats(documents);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取热门关键词
     */
    @GetMapping("/keywords/popular")
    @Operation(summary = "获取热门关键词", description = "获取文档中的热门关键词")
    public ResponseEntity<Map<String, Object>> getPopularKeywords(
            @RequestParam List<Document> documents,
            @RequestParam(defaultValue = "10") int topN) {

        List<Map.Entry<String, Long>> keywords = aiMetadataTransformer.getPopularKeywords(documents, topN);

        Map<String, Object> response = new HashMap<>();
        response.put("topN", topN);
        response.put("keywords", keywords);

        return ResponseEntity.ok(response);
    }

    // 请求DTO类
    @Data
    public static class BatchMetadataRequest {
        private List<Document> documents;
        private Map<String, DocumentMetadataDto> fileMetadata;
    }

    @Data
    public static class ComprehensiveProcessRequest {
        private List<Document> documents;
        private Map<String, DocumentMetadataDto> fileMetadata;
    }

    @Data
    public static class DocumentMetadataDto {
        private String title;
        private String author;
        private String category;
        private List<String> tags;
        private String description;
        private Map<String, Object> additionalFields;
    }
}
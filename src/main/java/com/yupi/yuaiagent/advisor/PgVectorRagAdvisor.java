package com.yupi.yuaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 简单的RAG Advisor实现，用于手动增强提示
 */
@Slf4j
public class PgVectorRagAdvisor {

    private final VectorStore vectorStore;
    private final SearchRequest searchRequest;

    public PgVectorRagAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.7)
                .build();
    }

    /**
     * 基于用户查询检索相关上下文
     */
    public String retrieveContext(String userQuery) {
        SearchRequest request = SearchRequest.builder()
                .query(userQuery)
                .topK(searchRequest.getTopK())
                .similarityThreshold(searchRequest.getSimilarityThreshold())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);

        if (!documents.isEmpty()) {
            String context = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));

            log.info("检索到 {} 个相关文档", documents.size());
            return "基于以下知识库内容回答问题：\n" + context + "\n\n";
        }

        return "";
    }
}
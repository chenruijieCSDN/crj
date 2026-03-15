package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 实现4：运用 Spring 内置文档检索器的 filterExpression 配置过滤规则
 *
 * 使用 VectorStoreDocumentRetriever 的 filterExpression，在编程中配置元数据过滤条件，
 * 仅检索满足条件的文档（如按 category、filename 等）。
 */
@Configuration
@Slf4j
public class RagFilteredRetrieverConfig {

    @Resource
    private VectorStore loveAppVectorStore;

    private static final FilterExpressionTextParser FILTER_PARSER = new FilterExpressionTextParser();

    /**
     * 带过滤规则的文档检索器：使用 filterExpression 配置过滤规则（示例：按 category）
     * 规则为 SQL 风格，如：category == '恋爱指导' || category == '恋爱技巧'
     */
    @Bean
    public DocumentRetriever filteredVectorStoreDocumentRetriever() {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(loveAppVectorStore)
                .topK(5)
                .similarityThreshold(0.7)
                .filterExpression(() -> FILTER_PARSER.parse("category == '恋爱指导' || category == '恋爱技巧' || category == '婚姻生活' || category == '单身指南'"))
                .build();
    }

    /**
     * 基于过滤检索器的 RAG 顾问（可用于本地向量库 + filterExpression 过滤规则）
     */
    @Bean
    public Advisor loveAppFilteredVectorStoreRagAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(filteredVectorStoreDocumentRetriever())
                .build();
    }
}

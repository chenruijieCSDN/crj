package com.yupi.yuaiagent.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * 测试专用的PgVector配置，使用不同的表名避免维度冲突
 */
@TestConfiguration
public class TestPgVectorConfig {

    @Bean
    @Primary
    public VectorStore testPgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        // 使用测试表名，避免与生产环境的1536维表冲突
        return PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)                    // DashScope输出1024维
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)              // 自动创建表
                .schemaName("public")
                .vectorTableName("vector_store_test") // 使用不同的表名
                .maxDocumentBatchSize(10000)
                .build();
    }
}
package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Import(TestPgVectorConfig.class) // 导入测试配置
public class PgVectorVectorStoreConfigTest {

    @Resource
    VectorStore vectorStore; // 使用测试配置中的vectorStore

    @Test
    void pgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));
        // 添加文档
        vectorStore.add(documents);
        // 相似度查询
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(5).build());
        Assertions.assertNotNull(results);
        // 验证结果不为空且包含预期内容
        Assertions.assertFalse(results.isEmpty());
        System.out.println("搜索结果数量: " + results.size());
        results.forEach(doc -> System.out.println("找到文档: " + doc.getText()));
    }
}

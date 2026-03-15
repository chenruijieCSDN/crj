# 文档元信息处理功能使用示例

本文档展示了如何使用增强的文档处理功能，包括批量添加元信息和AI自动添加元信息。

## 功能概述

1. **批量添加元信息 (EnhancedLoveAppDocumentLoader)**
   - 为文档批量添加标题、作者、分类、标签等元信息
   - 支持为不同文档配置不同的元信息
   - 提供文档统计和摘要功能

2. **AI自动添加元信息 (AiMetadataTransformer)**
   - 使用Spring AI的ChatClient自动分析文档内容
   - 生成标题、摘要、关键词、分类、情感倾向等元信息
   - 支持异步处理和统计功能

## 使用示例

### 示例1：批量添加元信息

```java
// 创建增强的文档加载器
EnhancedLoveAppDocumentLoader loader = new EnhancedLoveAppDocumentLoader(resourcePatternResolver);

// 为每个文件配置特定的元信息
Map<String, EnhancedLoveAppDocumentLoader.DocumentMetadata> metadataMap = new HashMap<>();

// 配置"恋爱篇"文档的元信息
EnhancedLoveAppDocumentLoader.DocumentMetadata loveMetadata = new EnhancedLoveAppDocumentLoader.DocumentMetadata();
loveMetadata.setTitle("恋爱技巧完全指南");
loveMetadata.setAuthor("恋爱专家团队");
loveMetadata.setCategory("恋爱技巧");
loveMetadata.setTags(Arrays.asList("恋爱", "约会", "表白", "相处"));
loveMetadata.setDescription("从相识到相恋的完整指导");
metadataMap.put("恋爱常见问题和回答 - 恋爱篇.md", loveMetadata);

// 批量加载文档并添加元信息
List<Document> documents = loader.loadMarkdownsWithMetadata(metadataMap);

// 获取处理统计
Map<String, Object> summary = loader.getDocumentSummary(documents);
System.out.println("处理统计: " + summary);
```

### 示例2：AI自动生成元信息

```java
// 首先加载基础文档
List<Document> documents = // ... 获取文档

// 使用AI自动生成元信息
List<Document> aiProcessedDocs = aiMetadataTransformer.apply(documents);

// 查看AI生成的元信息
Document doc = aiProcessedDocs.get(0);
System.out.println("AI标题: " + doc.getMetadata().get("ai_title"));
System.out.println("AI关键词: " + doc.getMetadata().get("ai_keywords"));
System.out.println("AI分类: " + doc.getMetadata().get("ai_category"));

// 获取处理统计
Map<String, Object> stats = aiMetadataTransformer.getProcessingStats(aiProcessedDocs);
System.out.println("处理统计: " + stats);

// 获取热门关键词
List<Map.Entry<String, Long>> keywords = aiMetadataTransformer.getPopularKeywords(aiProcessedDocs, 5);
keywords.forEach(kw -> System.out.println(kw.getKey() + ": " + kw.getValue() + "次"));
```

### 示例3：综合处理（批量+AI）

```java
// 使用综合处理器
DocumentProcessingConfig.LoveAppDocumentProcessor processor =
    new DocumentProcessingConfig().new LoveAppDocumentProcessor();

// 先批量添加基础元信息，再通过AI增强
List<Document> processedDocs = processor.processDocuments(documents, metadataMap);

// 或者异步处理
CompletableFuture<List<Document>> future = processor.processDocumentsAsync(documents, metadataMap);
List<Document> result = future.get(); // 等待处理完成
```

### 示例4：使用自定义Transformer

```java
// 创建自定义Transformer
DocumentProcessingConfig.CustomMetadataTransformer transformer =
    new DocumentProcessingConfig().new CustomMetadataTransformer(aiMetadataTransformer);

// 配置自定义元信息
EnhancedLoveAppDocumentLoader.DocumentMetadata customMetadata =
    new EnhancedLoveAppDocumentLoader.DocumentMetadata();
customMetadata.setTitle("自定义标题");
customMetadata.setAuthor("自定义作者");
// ... 设置其他属性

// 应用自定义处理
List<Document> result = transformer
    .withMetadata(customMetadata)
    .withAi(true) // 是否使用AI增强
    .apply(documents);
```

## REST API使用

### 批量添加元信息
```http
POST /api/documents/process/batch-metadata
Content-Type: application/json

{
    "fileMetadata": {
        "恋爱常见问题和回答 - 恋爱篇.md": {
            "title": "恋爱技巧完全指南",
            "author": "恋爱专家团队",
            "category": "恋爱技巧",
            "tags": ["恋爱", "约会", "表白"],
            "description": "从相识到相恋的完整指导"
        }
    }
}
```

### AI自动生成元信息
```http
POST /api/documents/process/ai-metadata
Content-Type: application/json

[
    {
        "text": "文档内容...",
        "metadata": {}
    }
]
```

### 综合处理
```http
POST /api/documents/process/comprehensive
Content-Type: application/json

{
    "documents": [...],
    "fileMetadata": {...}
}
```

## 元信息字段说明

### 基础元信息字段
- `filename`: 文件名
- `title`: 文档标题
- `author`: 作者
- `category`: 分类
- `tags`: 标签列表
- `description`: 描述
- `createTime`: 创建时间
- `contentLength`: 内容长度
- `wordCount`: 词数统计

### AI生成的元信息字段
- `ai_title`: AI生成的标题
- `ai_summary`: AI生成的摘要
- `ai_keywords`: AI提取的关键词
- `ai_category`: AI确定的分类
- `ai_sentiment`: AI判断的情感倾向
- `ai_topics`: AI识别的主题
- `ai_quality_score`: AI评估的内容质量分数
- `ai_processed`: 是否已AI处理
- `ai_processed_time`: AI处理时间

### 自定义字段示例
- `difficulty`: 难度等级（入门/进阶/高级）
- `applicableStage`: 适用阶段（单身/恋爱中/已婚）
- `contentType`: 内容类型（问答/指导/案例分析）
- `targetAudience`: 目标受众
- `estimatedReadingTime`: 预计阅读时间

## 最佳实践

1. **批量处理 vs AI处理**
   - 批量处理适合有明确元信息需求的场景
   - AI处理适合需要深度内容分析的场景
   - 综合使用可以获得最佳效果

2. **性能优化**
   - 大量文档时使用异步处理
   - 合理设置AI处理的文档长度限制
   - 缓存处理结果避免重复处理

3. **质量控制**
   - 检查AI生成的元信息质量
   - 设置合理的质量分数阈值
   - 对处理失败的文档进行人工审核

4. **扩展性**
   - 自定义元信息字段满足特定需求
   - 扩展AI提示词以生成更多类型的元信息
   - 根据业务需求调整处理流程
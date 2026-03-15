package com.yupi.yuaiagent.rag;

import org.springframework.ai.document.Document;
import java.util.List;

/**
 * 文档转换器接口
 * 由于Spring AI 1.1.2版本不包含Transformer接口，自定义实现
 */
@FunctionalInterface
public interface DocumentTransformer {

    /**
     * 转换文档列表
     * @param documents 要转换的文档列表
     * @return 转换后的文档列表
     */
    List<Document> apply(List<Document> documents);

    /**
     * 组合多个转换器
     */
    default DocumentTransformer andThen(DocumentTransformer after) {
        return documents -> after.apply(this.apply(documents));
    }
}
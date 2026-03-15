package com.yupi.yuaiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义基于阿里云知识库服务 RAG 增强顾问
 *
 * 实现2：多查询扩展（MultiQueryExpander，3-5 个查询，保留原查询语义）
 * 实现3：查询重写（RewriteQueryTransformer）+ 多语言翻译（TranslationQueryTransformer）
 * 实现5：上下文查询增强器（ContextualQueryAugmenter），允许空上下文并友好提示
 */
@Configuration
@Slf4j
class LoveAppRagCloudAdvisorConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashScopeApiKey;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    @Bean
    public Advisor loveAppRagCloudAdvisor() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();

        final String KNOWLEDGE_INDEX = "恋爱大师";
        DocumentRetriever documentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build());

        RetrievalAugmentationAdvisor.Builder builder = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever);

        if (chatClientBuilder != null) {
            // 实现2：多查询扩展，建议 3-5 个查询，保留原始查询核心语义
            MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
                    .chatClientBuilder(chatClientBuilder)
                    .numberOfQueries(4)
                    .includeOriginal(true)
                    .build();
            builder.queryExpander(multiQueryExpander);

            // 实现3：查询重写 + 多语言翻译
            RewriteQueryTransformer rewriteTransformer = RewriteQueryTransformer.builder()
                    .chatClientBuilder(chatClientBuilder)
                    .targetSearchSystem("恋爱指导知识库")
                    .build();
            TranslationQueryTransformer translationTransformer = TranslationQueryTransformer.builder()
                    .chatClientBuilder(chatClientBuilder)
                    .targetLanguage("chinese")
                    .build();
            builder.queryTransformers(rewriteTransformer, translationTransformer);

            // 实现5：允许空上下文，并提供友好空上下文提示，引导用户补充信息
            ContextualQueryAugmenter contextualQueryAugmenter = ContextualQueryAugmenter.builder()
                    .allowEmptyContext(true)
                    .emptyContextPromptTemplate(new PromptTemplate("""
                        未找到与您问题直接相关的知识库内容。可能原因：表述较模糊或知识库暂未覆盖该方面。
                        请尝试：1）换一种说法描述您的问题；2）补充更多背景（如：单身/恋爱中/已婚、具体场景）；3）或直接描述您的困扰，我会基于通用恋爱指导为您解答。
                        """))
                    .build();
            builder.queryAugmenter(contextualQueryAugmenter);
        }

        return builder.build();
    }
}

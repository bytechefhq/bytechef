/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayUrlValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayEmbeddingModelFactoryImpl implements AiGatewayEmbeddingModelFactory {

    public AiGatewayEmbeddingModelFactoryImpl() {
    }

    @Override
    @Cacheable(cacheNames = AI_GATEWAY_EMBEDDING_MODEL_CACHE, key = "#provider.id")
    public EmbeddingModel getEmbeddingModel(AiGatewayProvider provider) {
        return createEmbeddingModel(provider);
    }

    @Override
    @CacheEvict(cacheNames = AI_GATEWAY_EMBEDDING_MODEL_CACHE, key = "#providerId")
    public void evict(long providerId) {
    }

    @Override
    @CacheEvict(cacheNames = AI_GATEWAY_EMBEDDING_MODEL_CACHE, allEntries = true)
    public void evictAll() {
    }

    private EmbeddingModel createEmbeddingModel(AiGatewayProvider provider) {
        String apiKey = provider.revealApiKey();
        String baseUrl = provider.getBaseUrl();

        if (baseUrl != null && !baseUrl.isBlank()) {
            AiGatewayUrlValidator.validateExternalUrl(baseUrl);
        }

        return switch (provider.getType()) {
            case ANTHROPIC -> throw new UnsupportedOperationException(
                "Anthropic does not provide an embeddings API");
            case OPENAI, AZURE_OPENAI, COHERE, DEEPSEEK, GOOGLE_GEMINI, GROQ, MISTRAL ->
                createOpenAiCompatibleEmbeddingModel(apiKey, baseUrl, provider.getType());
        };
    }

    private EmbeddingModel createOpenAiCompatibleEmbeddingModel(
        String apiKey, String baseUrl, AiGatewayProviderType type) {
        String resolvedBaseUrl = AiGatewayProviderBaseUrls.resolveBaseUrl(baseUrl, type);

        OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl)
            .build();

        return new OpenAiEmbeddingModel(openAiClient);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.provider;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.security.AiGatewayUrlValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Creates and caches EmbeddingModel instances per provider. The {@code @Cacheable} on {@link #getEmbeddingModel} relies
 * on Spring AOP proxying — callers must invoke this method through the Spring-injected bean, not via an internal
 * {@code this} call, otherwise caching is silently bypassed and a new HTTP client is created per request.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayEmbeddingModelFactory {

    public static final String AI_GATEWAY_EMBEDDING_MODEL_CACHE = "ai-gateway-embedding-model";

    public AiGatewayEmbeddingModelFactory() {
    }

    @Cacheable(cacheNames = AI_GATEWAY_EMBEDDING_MODEL_CACHE, key = "#provider.id")
    public EmbeddingModel getEmbeddingModel(AiGatewayProvider provider) {
        return createEmbeddingModel(provider);
    }

    @CacheEvict(cacheNames = AI_GATEWAY_EMBEDDING_MODEL_CACHE, key = "#providerId")
    public void evict(long providerId) {
    }

    @CacheEvict(cacheNames = AI_GATEWAY_EMBEDDING_MODEL_CACHE, allEntries = true)
    public void evictAll() {
    }

    private EmbeddingModel createEmbeddingModel(AiGatewayProvider provider) {
        String apiKey = provider.revealApiKey();
        String baseUrl = provider.getBaseUrl();

        // Defense-in-depth SSRF guard — see AiGatewayChatModelFactory#createChatModel for rationale.
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

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl)
            .build();

        return new OpenAiEmbeddingModel(openAiApi);
    }
}

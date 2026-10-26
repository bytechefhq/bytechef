/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.util.AiGatewayUrlValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
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
public class AiGatewayChatModelFactoryImpl implements AiGatewayChatModelFactory {

    public AiGatewayChatModelFactoryImpl() {
    }

    @Override
    @Cacheable(cacheNames = AI_GATEWAY_CHAT_MODEL_CACHE, key = "#provider.id", sync = true)
    public ChatModel getChatModel(AiGatewayProvider provider) {
        return createChatModel(provider);
    }

    @Override
    @CacheEvict(cacheNames = AI_GATEWAY_CHAT_MODEL_CACHE, key = "#providerId")
    public void evict(long providerId) {
    }

    @Override
    @CacheEvict(cacheNames = AI_GATEWAY_CHAT_MODEL_CACHE, allEntries = true)
    public void evictAll() {
    }

    private ChatModel createChatModel(AiGatewayProvider provider) {
        String apiKey = provider.revealApiKey();
        String baseUrl = provider.getBaseUrl();

        if (baseUrl != null && !baseUrl.isBlank()) {
            AiGatewayUrlValidator.validateExternalUrl(baseUrl);
        }

        return switch (provider.getType()) {
            case ANTHROPIC -> createAnthropicChatModel(apiKey, baseUrl);
            case OPENAI, AZURE_OPENAI, COHERE, DEEPSEEK, GOOGLE_GEMINI, GROQ, MISTRAL ->
                createOpenAiCompatibleChatModel(apiKey, baseUrl, provider.getType());
        };
    }

    private ChatModel createAnthropicChatModel(String apiKey, String baseUrl) {
        AnthropicOkHttpClient.Builder anthropicClientBuilder = AnthropicOkHttpClient.builder()
            .apiKey(apiKey);

        if (baseUrl != null && !baseUrl.isEmpty()) {
            anthropicClientBuilder.baseUrl(baseUrl);
        }

        return AnthropicChatModel.builder()
            .anthropicClient(anthropicClientBuilder.build())
            .build();
    }

    private ChatModel createOpenAiCompatibleChatModel(
        String apiKey, String baseUrl, AiGatewayProviderType type) {
        String resolvedBaseUrl = AiGatewayProviderBaseUrls.resolveBaseUrl(baseUrl, type);

        OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl)
            .build();

        OpenAIClientAsync openAiClientAsync = OpenAIOkHttpClientAsync.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl)
            .build();

        return OpenAiChatModel.builder()
            .openAiClient(openAiClient)
            .openAiClientAsync(openAiClientAsync)
            .build();
    }
}

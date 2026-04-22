/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.provider;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.automation.ai.gateway.security.AiGatewayUrlValidator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Creates and caches ChatModel instances per provider. The {@code @Cacheable} on {@link #getChatModel} relies on Spring
 * AOP proxying — callers must invoke this method through the Spring-injected bean, not via an internal {@code this}
 * call, otherwise caching is silently bypassed and a new HTTP client is created per request.
 *
 * <p>
 * <b>Cross-tenant safety:</b> {@link AiGatewayProvider} is a <i>global</i> row (not workspace-scoped); each provider
 * carries a single encrypted API key shared by all workspaces linked to it via {@code WorkspaceAiGatewayProvider}.
 * Caching by {@code provider.id} alone is therefore correct — there is no per-workspace API-key variance to preserve.
 * Workspace-isolation is enforced upstream in {@code AiGatewayFacade.validateWorkspaceAccess}: a request tag-spoofing
 * another workspace's id is rejected before it ever reaches this factory. If provider-ownership ever becomes
 * per-workspace, the cache key must be widened to {@code workspaceId + ":" + provider.id} in the same commit that makes
 * that change.
 * </p>
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayChatModelFactory {

    public static final String AI_GATEWAY_CHAT_MODEL_CACHE = "ai-gateway-chat-model";

    public AiGatewayChatModelFactory() {
    }

    /**
     * {@code sync = true} serializes concurrent misses on the same key — without it, a cache stampede at startup (N
     * concurrent first-requests) fires N provider client instantiations, each opening its own HTTP connection pool.
     * Under sync, only the first caller builds the ChatModel; subsequent callers block on the same cache lookup and
     * return the freshly populated entry.
     */
    @Cacheable(cacheNames = AI_GATEWAY_CHAT_MODEL_CACHE, key = "#provider.id", sync = true)
    public ChatModel getChatModel(AiGatewayProvider provider) {
        return createChatModel(provider);
    }

    @CacheEvict(cacheNames = AI_GATEWAY_CHAT_MODEL_CACHE, key = "#providerId")
    public void evict(long providerId) {
    }

    @CacheEvict(cacheNames = AI_GATEWAY_CHAT_MODEL_CACHE, allEntries = true)
    public void evictAll() {
    }

    private ChatModel createChatModel(AiGatewayProvider provider) {
        String apiKey = provider.revealApiKey();
        String baseUrl = provider.getBaseUrl();

        // Defense-in-depth SSRF guard. The facade validates baseUrl at write time, but a stale row predating the
        // validator — or any path that bypasses the facade — must not be able to direct the Authorization header at a
        // private target. Default-per-provider base URLs (resolved below for the OpenAI family) are trusted constants.
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

        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(apiKey)
            .baseUrl(resolvedBaseUrl)
            .build();

        return OpenAiChatModel.builder()
            .openAiApi(openAiApi)
            .build();
    }
}

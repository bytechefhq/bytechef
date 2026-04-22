/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.cache;

import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionRequest;
import com.bytechef.ee.automation.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
public class AiGatewayResponseCache {

    public static final String AI_GATEWAY_RESPONSE_CACHE = "ai-gateway-response";
    private static final Logger logger = LoggerFactory.getLogger(AiGatewayResponseCache.class);

    private final ObjectMapper objectMapper;

    public AiGatewayResponseCache(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Cacheable(cacheNames = AI_GATEWAY_RESPONSE_CACHE, key = "#cacheKey", unless = "#result == null")
    public AiGatewayChatCompletionResponse get(String cacheKey) {
        return null;
    }

    @CachePut(cacheNames = AI_GATEWAY_RESPONSE_CACHE, key = "#cacheKey")
    public AiGatewayChatCompletionResponse put(
        String cacheKey, AiGatewayChatCompletionResponse response) {

        return response;
    }

    public String computeCacheKey(AiGatewayChatCompletionRequest request) {
        String messagesJson;

        try {
            messagesJson = objectMapper.writeValueAsString(request.messages());
        } catch (JacksonException jacksonException) {
            throw new IllegalStateException(
                "Failed to serialize messages for cache key computation", jacksonException);
        }

        String toolsJson;

        try {
            toolsJson = request.tools() != null ? objectMapper.writeValueAsString(request.tools()) : "";
        } catch (JacksonException jacksonException) {
            throw new IllegalStateException(
                "Failed to serialize tools for cache key computation", jacksonException);
        }

        String toolChoiceJson;

        try {
            toolChoiceJson =
                request.toolChoice() != null ? objectMapper.writeValueAsString(request.toolChoice()) : "";
        } catch (JacksonException jacksonException) {
            throw new IllegalStateException(
                "Failed to serialize toolChoice for cache key computation", jacksonException);
        }

        String raw = request.model() + "|" + messagesJson + "|" +
            request.temperature() + "|" + request.maxTokens() + "|" + request.topP() + "|" +
            toolsJson + "|" + toolChoiceJson;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                .formatHex(hash);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 not available", noSuchAlgorithmException);
        }
    }

    public boolean shouldCache(AiGatewayChatCompletionRequest request) {
        if (Boolean.FALSE.equals(request.cache())) {
            return false;
        }

        if (request.stream()) {
            return false;
        }

        return request.temperature() == null || Double.compare(request.temperature(), 0.0) == 0;
    }
}

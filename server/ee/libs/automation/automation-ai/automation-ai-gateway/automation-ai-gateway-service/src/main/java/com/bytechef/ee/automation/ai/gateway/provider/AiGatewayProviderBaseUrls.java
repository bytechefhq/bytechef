/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.provider;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayProviderType;

/**
 * @version ee
 */
final class AiGatewayProviderBaseUrls {

    private AiGatewayProviderBaseUrls() {
    }

    static String resolveBaseUrl(String baseUrl, AiGatewayProviderType type) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl;
        }

        return switch (type) {
            case AZURE_OPENAI -> throw new IllegalArgumentException(
                "Base URL is required for Azure OpenAI (e.g., https://{resource}.openai.azure.com)");
            case COHERE -> "https://api.cohere.com/compatibility";
            case DEEPSEEK -> "https://api.deepseek.com";
            case GOOGLE_GEMINI -> "https://generativelanguage.googleapis.com";
            case GROQ -> "https://api.groq.com/openai";
            case MISTRAL -> "https://api.mistral.ai";
            case OPENAI -> "https://api.openai.com";
            case ANTHROPIC -> throw new IllegalArgumentException("Anthropic uses its own API, not OpenAI-compatible");
        };
    }
}

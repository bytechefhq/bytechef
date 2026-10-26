/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AiModelsDevProviderIds {

    private AiModelsDevProviderIds() {
    }

    static String resolveProviderId(AiGatewayProviderType type) {
        return switch (type) {
            case ANTHROPIC -> "anthropic";
            case AZURE_OPENAI -> "azure";
            case COHERE -> "cohere";
            case DEEPSEEK -> "deepseek";
            case GOOGLE_GEMINI -> "google";
            case GROQ -> "groq";
            case MISTRAL -> "mistral";
            case OPENAI -> "openai";
        };
    }
}

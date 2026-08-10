/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;

/**
 * Maps the gateway's provider types onto models.dev provider ids.
 *
 * <p>
 * The switch is deliberately exhaustive with no {@code default} arm: appending a value to {@link AiGatewayProviderType}
 * then breaks compilation here, which is the point. A {@code default} returning null would let a new provider ship
 * silently reconciling nothing.
 *
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

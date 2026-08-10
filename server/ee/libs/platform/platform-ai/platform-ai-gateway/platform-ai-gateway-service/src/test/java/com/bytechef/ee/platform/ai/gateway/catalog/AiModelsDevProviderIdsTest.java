/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiModelsDevProviderIdsTest {

    @Test
    void testResolveProviderIdMapsEveryKnownType() {
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.ANTHROPIC))
            .isEqualTo("anthropic");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.AZURE_OPENAI))
            .isEqualTo("azure");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.COHERE))
            .isEqualTo("cohere");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.DEEPSEEK))
            .isEqualTo("deepseek");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.GOOGLE_GEMINI))
            .isEqualTo("google");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.GROQ))
            .isEqualTo("groq");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.MISTRAL))
            .isEqualTo("mistral");
        assertThat(AiModelsDevProviderIds.resolveProviderId(AiGatewayProviderType.OPENAI))
            .isEqualTo("openai");
    }

    /**
     * Fails the build when someone appends a provider type without mapping it, rather than letting that provider
     * silently reconcile nothing at runtime.
     */
    @Test
    void testEveryProviderTypeHasAMapping() {
        for (AiGatewayProviderType type : AiGatewayProviderType.values()) {
            assertThat(AiModelsDevProviderIds.resolveProviderId(type))
                .as("no models.dev provider id mapped for %s", type)
                .isNotBlank();
        }
    }
}

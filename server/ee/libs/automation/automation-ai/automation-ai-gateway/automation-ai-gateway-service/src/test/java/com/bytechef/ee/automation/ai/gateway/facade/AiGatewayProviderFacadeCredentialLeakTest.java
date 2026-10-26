/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiGatewayProviderFacadeCredentialLeakTest {

    private static final long PROVIDER_ID = 99L;

    @Mock
    private AiGatewayProviderService aiGatewayProviderService;

    @Test
    void testGetProviderNeverSerializesThePlaintextApiKey() {
        String plaintextApiKey = "sk-super-secret-" + System.nanoTime();
        AiGatewayProvider provider = new AiGatewayProvider(
            "byok-leak-test-provider", AiGatewayProviderType.OPENAI, plaintextApiKey);

        ReflectionTestUtils.setField(provider, "id", PROVIDER_ID);

        when(aiGatewayProviderService.getProvider(PROVIDER_ID)).thenReturn(provider);

        AiGatewayProviderFacade aiGatewayProviderFacade = new AiGatewayProviderFacadeImpl(aiGatewayProviderService);

        AiGatewayProvider result = aiGatewayProviderFacade.getProvider(PROVIDER_ID);

        String json = new ObjectMapper().writeValueAsString(result);

        assertThat(json)
            .as("a provider's plaintext API key must never be present in the serialized form returned by a read "
                + "path — a customer's BYOK credential is exactly as sensitive as the tenant's own")
            .doesNotContain(plaintextApiKey);
        assertThat(json)
            .as("no apiKey-shaped field should be present at all; getApiKeyWrapped/revealApiKey are both "
                + "@JsonIgnore for exactly this reason")
            .doesNotContainIgnoringCase("apiKey");
    }
}

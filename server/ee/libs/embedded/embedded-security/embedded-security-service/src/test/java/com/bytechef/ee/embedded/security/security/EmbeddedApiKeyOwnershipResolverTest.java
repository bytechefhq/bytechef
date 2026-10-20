/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyOwnershipResolverTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final EmbeddedApiKeyOwnershipResolver resolver = new EmbeddedApiKeyOwnershipResolver(apiKeyService);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("ApiKey");
    }

    @Test
    void testResolvesAnEmbeddedKeyToItsOwner() {
        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.of(apiKey(PlatformType.EMBEDDED)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofEmbedded(42L));
        assertThat(resolver.resolveOwner(Long.valueOf(7L))).isEqualTo(ResourceOwner.ofEmbedded(42L));
    }

    @Test
    void testANullTypeKeyIsNotClaimedByTheEmbeddedResolver() {
        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.of(apiKey(null)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAnAutomationKeyIsUnknown() {
        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.of(apiKey(PlatformType.AUTOMATION)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANonNumericIdIsUnknown() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    private static ApiKey apiKey(PlatformType type) {
        ApiKey apiKey = new ApiKey();

        apiKey.setType(type);
        apiKey.setUserId(42L);

        return apiKey;
    }
}

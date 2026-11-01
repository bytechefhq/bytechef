/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiPlatformApiKeyAuthenticationProviderTest {

    @Test
    void testAuthenticatedTokenCarriesTheApiKeyId() {
        ApiKey apiKey = mock(ApiKey.class);

        when(apiKey.getId()).thenReturn(7L);
        when(apiKey.getName()).thenReturn("orders");

        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.getApiKey("secret", 0L)).thenReturn(apiKey);

        ApiPlatformApiKeyAuthenticationProvider apiPlatformApiKeyAuthenticationProvider =
            new ApiPlatformApiKeyAuthenticationProvider(apiKeyService);

        Authentication authentication = apiPlatformApiKeyAuthenticationProvider.authenticate(
            new ApiPlatformApiKeyAuthenticationToken(0L, "secret", "public"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("orders");
        assertThat(((ApiPlatformApiKeyAuthenticationToken) authentication).getApiKeyId()).isEqualTo(7L);
    }
}

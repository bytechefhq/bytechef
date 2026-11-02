/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.handler.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
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

        when(apiKeyService.fetchApiKey("secret", 0L)).thenReturn(Optional.of(apiKey));

        ApiPlatformApiKeyAuthenticationProvider apiPlatformApiKeyAuthenticationProvider =
            new ApiPlatformApiKeyAuthenticationProvider(apiKeyService);

        Authentication authentication = apiPlatformApiKeyAuthenticationProvider.authenticate(
            new ApiPlatformApiKeyAuthenticationToken(0L, "secret", "public"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("orders");
        assertThat(((ApiPlatformApiKeyAuthenticationToken) authentication).getApiKeyId()).isEqualTo(7L);
    }

    @Test
    void testUnknownSecretKeyIsRejectedAsBadCredentials() {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.fetchApiKey("secret", 0L)).thenReturn(Optional.empty());

        ApiPlatformApiKeyAuthenticationProvider apiPlatformApiKeyAuthenticationProvider =
            new ApiPlatformApiKeyAuthenticationProvider(apiKeyService);

        ApiPlatformApiKeyAuthenticationToken apiPlatformApiKeyAuthenticationToken =
            new ApiPlatformApiKeyAuthenticationToken(0L, "secret", "public");

        assertThatThrownBy(
            () -> apiPlatformApiKeyAuthenticationProvider.authenticate(apiPlatformApiKeyAuthenticationToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unknown API secret key");
    }

    @Test
    void testAFailedApiKeyLookupPropagates() {
        ApiKeyService apiKeyService = mock(ApiKeyService.class);

        when(apiKeyService.fetchApiKey("secret", 0L)).thenThrow(new IllegalStateException("database down"));

        ApiPlatformApiKeyAuthenticationProvider apiPlatformApiKeyAuthenticationProvider =
            new ApiPlatformApiKeyAuthenticationProvider(apiKeyService);

        ApiPlatformApiKeyAuthenticationToken apiPlatformApiKeyAuthenticationToken =
            new ApiPlatformApiKeyAuthenticationToken(0L, "secret", "public");

        assertThatThrownBy(
            () -> apiPlatformApiKeyAuthenticationProvider.authenticate(apiPlatformApiKeyAuthenticationToken))
                .isInstanceOf(IllegalStateException.class);
    }
}

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

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

        when(apiKey.getType()).thenReturn(PlatformType.AUTOMATION);

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

    @Nested
    @SuppressFBWarnings("HARD_CODE_PASSWORD")
    class PlatformTypeEnforcement {

        private static final long ENVIRONMENT_ID = 1L;
        private static final String SECRET_KEY = "secret-key";

        private ApiKeyService apiKeyService;
        private ApiPlatformApiKeyAuthenticationProvider apiPlatformApiKeyAuthenticationProvider;

        @BeforeEach
        void setUp() {
            apiKeyService = mock(ApiKeyService.class);

            apiPlatformApiKeyAuthenticationProvider = new ApiPlatformApiKeyAuthenticationProvider(apiKeyService);
        }

        @Test
        void testRejectsAPlatformKey() {
            when(apiKeyService.fetchApiKey(SECRET_KEY, ENVIRONMENT_ID))
                .thenReturn(Optional.of(createApiKey(null)));

            assertThatThrownBy(() -> apiPlatformApiKeyAuthenticationProvider.authenticate(
                new ApiPlatformApiKeyAuthenticationToken(ENVIRONMENT_ID, SECRET_KEY, "tenant")))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void testRejectsAnEmbeddedKey() {
            when(apiKeyService.fetchApiKey(SECRET_KEY, ENVIRONMENT_ID))
                .thenReturn(Optional.of(createApiKey(PlatformType.EMBEDDED)));

            assertThatThrownBy(() -> apiPlatformApiKeyAuthenticationProvider.authenticate(
                new ApiPlatformApiKeyAuthenticationToken(ENVIRONMENT_ID, SECRET_KEY, "tenant")))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void testAcceptsAnAutomationKey() {
            when(apiKeyService.fetchApiKey(SECRET_KEY, ENVIRONMENT_ID))
                .thenReturn(Optional.of(createApiKey(PlatformType.AUTOMATION)));

            Authentication authentication = apiPlatformApiKeyAuthenticationProvider.authenticate(
                new ApiPlatformApiKeyAuthenticationToken(ENVIRONMENT_ID, SECRET_KEY, "tenant"));

            assertThat(authentication.getName()).isEqualTo("key");
        }

        private static ApiKey createApiKey(PlatformType type) {
            ApiKey apiKey = new ApiKey();

            apiKey.setName("key");
            apiKey.setSecretKey(SECRET_KEY);
            apiKey.setType(type);

            return apiKey;
        }
    }
}

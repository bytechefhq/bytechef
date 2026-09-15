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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class ApiPlatformApiKeyAuthenticationProviderTest {

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
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(createApiKey(null));

        assertThatThrownBy(() -> apiPlatformApiKeyAuthenticationProvider.authenticate(
            new ApiPlatformApiKeyAuthenticationToken(ENVIRONMENT_ID, SECRET_KEY, "tenant")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testRejectsAnEmbeddedKey() {
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(createApiKey(PlatformType.EMBEDDED));

        assertThatThrownBy(() -> apiPlatformApiKeyAuthenticationProvider.authenticate(
            new ApiPlatformApiKeyAuthenticationToken(ENVIRONMENT_ID, SECRET_KEY, "tenant")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testAcceptsAnAutomationKey() {
        when(apiKeyService.getApiKey(SECRET_KEY, ENVIRONMENT_ID)).thenReturn(createApiKey(PlatformType.AUTOMATION));

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

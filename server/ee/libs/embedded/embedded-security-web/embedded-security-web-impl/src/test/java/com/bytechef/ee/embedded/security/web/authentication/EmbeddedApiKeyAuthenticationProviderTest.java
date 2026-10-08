/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationProviderTest {

    private static final long ENVIRONMENT_ID = 1L;
    private static final String EXTERNAL_USER_ID = "external-user";
    private static final String SECRET_KEY = "secret-key";

    private ApiKeyService apiKeyService;
    private ConnectedUserService connectedUserService;
    private EmbeddedApiKeyAuthenticationProvider embeddedApiKeyAuthenticationProvider;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        connectedUserService = mock(ConnectedUserService.class);

        embeddedApiKeyAuthenticationProvider =
            new EmbeddedApiKeyAuthenticationProvider(apiKeyService, connectedUserService);
    }

    @Test
    void testRejectsAKeyOfAnotherType() {
        when(apiKeyService.exists(eq(SECRET_KEY), eq(ENVIRONMENT_ID), any()))
            .thenAnswer(invocation -> invocation.getArgument(2) != PlatformType.EMBEDDED);

        assertThatThrownBy(() -> embeddedApiKeyAuthenticationProvider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, SECRET_KEY, "tenant")))
                .isInstanceOf(BadCredentialsException.class);

        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testAcceptsAnEmbeddedKey() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(apiKeyService.exists(SECRET_KEY, ENVIRONMENT_ID, PlatformType.EMBEDDED)).thenReturn(true);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        Authentication authentication = embeddedApiKeyAuthenticationProvider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, SECRET_KEY, "tenant"));

        assertThat(authentication.getName()).isEqualTo(EXTERNAL_USER_ID);
    }

    @Test
    void testAuthenticatedTokenCarriesConnectedUserAndEnvironment() {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, true, "ext-1", 42L, null, 0);

        when(apiKeyService.exists("secret", 2L, PlatformType.EMBEDDED)).thenReturn(true);
        when(connectedUserService.fetchConnectedUser("ext-1", 2L)).thenReturn(Optional.of(connectedUser));

        Authentication authentication = embeddedApiKeyAuthenticationProvider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(2L, "ext-1", "secret", "public"));

        assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);

        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        assertThat(connectedUserAuthentication.connectedUserId()).isEqualTo(42L);
        assertThat(connectedUserAuthentication.externalUserId()).isEqualTo("ext-1");
        assertThat(connectedUserAuthentication.environmentId()).isEqualTo(2L);
        assertThat(connectedUserAuthentication.apiKeyAuthenticated()).isTrue();
    }

    @Test
    void testAConnectedUserTokenIsNotApiKeyAuthenticated() {
        ConnectedUser connectedUser = new ConnectedUser(Map.of(), null, true, "ext-1", 42L, null, 0);

        when(connectedUserService.fetchConnectedUser("ext-1", 2L)).thenReturn(Optional.of(connectedUser));

        Authentication authentication = embeddedApiKeyAuthenticationProvider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(2L, "ext-1", null, "public"));

        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        assertThat(connectedUserAuthentication.apiKeyAuthenticated()).isFalse();

        verify(apiKeyService, never()).exists(anyString(), anyLong(), any());
    }
}

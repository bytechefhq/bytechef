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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationProviderTest {

    private static final String EXTERNAL_USER_ID = "customer-z";
    private static final long ENVIRONMENT_ID = 2L;

    private ApiKeyService apiKeyService;
    private ConnectedUserService connectedUserService;
    private EmbeddedApiKeyAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        connectedUserService = mock(ConnectedUserService.class);

        provider = new EmbeddedApiKeyAuthenticationProvider(apiKeyService, connectedUserService);

        when(apiKeyService.exists("secret", ENVIRONMENT_ID, PlatformType.EMBEDDED))
            .thenReturn(true);
    }

    @Test
    void testAuthenticationReusesAnExistingConnectedUser() {
        ConnectedUser existingConnectedUser = connectedUser(42L);

        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(existingConnectedUser));

        Authentication authentication = provider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, "secret", "tenant"));

        verify(connectedUserService, never()).createConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID);

        EmbeddedApiKeyAuthenticationToken authenticationToken = (EmbeddedApiKeyAuthenticationToken) authentication;

        assertThat(authenticationToken.connectedUserId()).isEqualTo(42L);
        assertThat(authenticationToken.externalUserId()).isEqualTo(EXTERNAL_USER_ID);
        assertThat(authenticationToken.environmentId()).isEqualTo(ENVIRONMENT_ID);
    }

    @Test
    void testAuthenticationReusesTheConnectedUserAConcurrentRequestCreatedFirst() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(connectedUser(44L)));
        when(connectedUserService.createConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenThrow(new DuplicateKeyException("uk_connected_user_environment_external_id"));

        Authentication authentication = provider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, "secret", "tenant"));

        EmbeddedApiKeyAuthenticationToken authenticationToken = (EmbeddedApiKeyAuthenticationToken) authentication;

        assertThat(authenticationToken.connectedUserId()).isEqualTo(44L);
    }

    @Test
    void testAuthenticationCreatesAConnectedUserOnFirstCall() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.empty());
        when(connectedUserService.createConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(connectedUser(43L));

        Authentication authentication = provider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, "secret", "tenant"));

        verify(connectedUserService).createConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID);

        EmbeddedApiKeyAuthenticationToken authenticationToken = (EmbeddedApiKeyAuthenticationToken) authentication;

        assertThat(authenticationToken.connectedUserId()).isEqualTo(43L);
        assertThat(authenticationToken.externalUserId()).isEqualTo(EXTERNAL_USER_ID);
    }

    @Test
    void testRejectsAKeyOfAnotherType() {
        when(apiKeyService.exists(eq("secret"), eq(ENVIRONMENT_ID), any()))
            .thenAnswer(invocation -> invocation.getArgument(2) != PlatformType.EMBEDDED);

        assertThatThrownBy(() -> provider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, "secret", "tenant")))
                .isInstanceOf(BadCredentialsException.class);

        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testAuthenticatedTokenCarriesConnectedUserAndEnvironment() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser(42L)));

        Authentication authentication = provider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, "secret", "tenant"));

        assertThat(authentication).isInstanceOf(ConnectedUserAuthentication.class);

        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        assertThat(connectedUserAuthentication.connectedUserId()).isEqualTo(42L);
        assertThat(connectedUserAuthentication.externalUserId()).isEqualTo(EXTERNAL_USER_ID);
        assertThat(connectedUserAuthentication.environmentId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(connectedUserAuthentication.apiKeyAuthenticated()).isTrue();
    }

    @Test
    void testAConnectedUserTokenIsNotApiKeyAuthenticated() {
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser(42L)));

        Authentication authentication = provider.authenticate(
            new EmbeddedApiKeyAuthenticationToken(ENVIRONMENT_ID, EXTERNAL_USER_ID, null, "tenant"));

        ConnectedUserAuthentication connectedUserAuthentication = (ConnectedUserAuthentication) authentication;

        assertThat(connectedUserAuthentication.apiKeyAuthenticated()).isFalse();

        verify(apiKeyService, never()).exists(anyString(), anyLong(), any());
    }

    private static ConnectedUser connectedUser(long id) {
        return new ConnectedUser(Map.of(), null, true, EXTERNAL_USER_ID, id, null, 0);
    }
}

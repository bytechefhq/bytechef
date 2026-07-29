/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerApiKeyAuthenticationProviderTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final long ENVIRONMENT_ID = 0L;
    private static final String MCP_SERVER_SECRET_KEY = "server-secret";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final EmbeddedMcpServerApiKeyAuthenticationProvider provider =
        new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService, mcpServerService);

    @Test
    void testAuthenticateResolvesEnabledConnectedUser() {
        mockMcpServer(true);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        Authentication authentication = provider.authenticate(
            new EmbeddedMcpServerApiKeyAuthenticationToken(
                ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(EXTERNAL_USER_ID);
    }

    @Test
    void testAuthenticateRejectsDisabledConnectedUser() {
        mockMcpServer(true);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(false);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        assertThatExceptionOfType(UserNotActivatedException.class)
            .isThrownBy(() -> provider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY)));
    }

    @Test
    void testAuthenticateRejectsMissingTokenWhenAuthenticationRequired() {
        mockMcpServer(true);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> provider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, null, "public", MCP_SERVER_SECRET_KEY)));
    }

    @Test
    void testAuthenticateReturnsAnonymousWhenAuthenticationNotRequired() {
        mockMcpServer(false);

        Authentication authentication = provider.authenticate(
            new EmbeddedMcpServerApiKeyAuthenticationToken(ENVIRONMENT_ID, null, "public", MCP_SERVER_SECRET_KEY));

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(MCP_SERVER_SECRET_KEY);
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void testSupportsOnlyApiKeyToken() {
        assertThat(provider.supports(EmbeddedMcpServerApiKeyAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(EmbeddedMcpServerOAuth2AuthenticationToken.class)).isFalse();
    }

    private void mockMcpServer(boolean authenticationRequired) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }
}

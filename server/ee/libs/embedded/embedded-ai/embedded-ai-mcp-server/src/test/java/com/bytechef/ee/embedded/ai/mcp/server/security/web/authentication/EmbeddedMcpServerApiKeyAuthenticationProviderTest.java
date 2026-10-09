/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.PlatformType;
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

    private static final long ENVIRONMENT_ID = 2L;
    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final long MCP_SERVER_ID = 1050L;
    private static final String MCP_SERVER_SECRET_KEY = "server-secret";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final EmbeddedMcpServerApiKeyAuthenticationProvider embeddedMcpServerApiKeyAuthenticationProvider =
        new EmbeddedMcpServerApiKeyAuthenticationProvider(connectedUserService, mcpServerService);

    @Test
    void testAuthenticateReturnsAnonymousWhenAuthenticationNotRequired() {
        mockMcpServer(PlatformType.EMBEDDED, false);

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
            new EmbeddedMcpServerApiKeyAuthenticationToken(ENVIRONMENT_ID, null, "public", MCP_SERVER_SECRET_KEY));

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("mcp-anonymous:embedded:" + MCP_SERVER_ID);
        assertThat(authentication.getAuthorities()).isEmpty();
        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testAuthenticateIgnoresPresentedTokenWhenAuthenticationNotRequired() {
        mockMcpServer(PlatformType.EMBEDDED, false);

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
            new EmbeddedMcpServerApiKeyAuthenticationToken(
                ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY));

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testAuthenticateRejectsMissingTokenWhenAuthenticationRequired() {
        mockMcpServer(PlatformType.EMBEDDED, true);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, null, "public", MCP_SERVER_SECRET_KEY)));
    }

    @Test
    void testAuthenticateRejectsUnknownMcpServerSecretKey() {
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY))
            .thenThrow(new IllegalArgumentException("MCP server for the given secret key not found"));

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY)));
    }

    @Test
    void testAuthenticateRejectsDisabledMcpServerWhenAuthenticationNotRequired() {
        mockMcpServer(PlatformType.EMBEDDED, false, false);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, null, "public", MCP_SERVER_SECRET_KEY)))
            .withMessage("MCP server is disabled");
    }

    @Test
    void testAuthenticateRejectsDisabledMcpServerWhenAuthenticationRequired() {
        mockMcpServer(PlatformType.EMBEDDED, true, false);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY)))
            .withMessage("MCP server is disabled");

        verify(connectedUserService, never()).fetchConnectedUser(anyString(), anyLong());
    }

    @Test
    void testAuthenticateRejectsNonEmbeddedMcpServer() {
        mockMcpServer(PlatformType.AUTOMATION, false);

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, null, "public", MCP_SERVER_SECRET_KEY)));
    }

    @Test
    void testAuthenticateResolvesEnabledConnectedUserWhenAuthenticationRequired() {
        mockMcpServer(PlatformType.EMBEDDED, true);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        Authentication authentication = embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
            new EmbeddedMcpServerApiKeyAuthenticationToken(
                ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY));

        assertThat(authentication).isInstanceOf(EmbeddedMcpServerApiKeyAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(EXTERNAL_USER_ID);
    }

    @Test
    void testAuthenticateRejectsDisabledConnectedUser() {
        mockMcpServer(PlatformType.EMBEDDED, true);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(false);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        assertThatExceptionOfType(UserNotActivatedException.class)
            .isThrownBy(() -> embeddedMcpServerApiKeyAuthenticationProvider.authenticate(
                new EmbeddedMcpServerApiKeyAuthenticationToken(
                    ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY)));
    }

    private void mockMcpServer(PlatformType type, boolean authenticationRequired) {
        mockMcpServer(type, authenticationRequired, true);
    }

    private void mockMcpServer(PlatformType type, boolean authenticationRequired, boolean enabled) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getId()).thenReturn(MCP_SERVER_ID);
        when(mcpServer.getType()).thenReturn(type);
        when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
        when(mcpServer.isEnabled()).thenReturn(enabled);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }
}

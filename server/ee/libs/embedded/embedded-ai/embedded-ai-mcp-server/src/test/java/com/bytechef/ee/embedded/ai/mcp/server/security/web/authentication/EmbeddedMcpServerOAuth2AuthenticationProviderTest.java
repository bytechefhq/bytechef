/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerOAuth2AuthenticationProviderTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final long ENVIRONMENT_ID = 0L;
    private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
    private static final String MCP_SERVER_SECRET_KEY = "server-secret";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final EmbeddedMcpServerOAuth2AuthenticationProvider provider =
        new EmbeddedMcpServerOAuth2AuthenticationProvider(connectedUserService, mcpServerService);

    @Test
    void testAuthenticateResolvesEnabledConnectedUser() {
        mockMcpServer(true);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        List<GrantedAuthority> mappedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_EDITOR"));

        Authentication authentication = provider.authenticate(
            new EmbeddedMcpServerOAuth2AuthenticationToken(
                ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY, mappedAuthorities));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(EXTERNAL_USER_ID);
        assertThat(
            authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList()).containsExactly("ROLE_EDITOR");
    }

    /**
     * Ticket 1051, same as the api-key provider: the AUTHENTICATED token lands in the SecurityContext and
     * {@code ConnectedUserResourceMembershipResolver} reads the caller's environment off it. Built with the
     * {@code User}-only constructor it carried none, so the ordinal read as 0 -- DEVELOPMENT, a valid environment
     * rather than an absent one -- for every caller. Uses a non-zero environment so the fabricated and real values do
     * not coincide.
     */
    @Test
    void testAuthenticatedTokenCarriesTheRequestEnvironment() {
        mockMcpServer(true);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.isEnabled()).thenReturn(true);
        when(connectedUser.getExternalId()).thenReturn(EXTERNAL_USER_ID);
        when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(Optional.of(connectedUser));

        Authentication authentication = provider.authenticate(
            new EmbeddedMcpServerOAuth2AuthenticationToken(
                PRODUCTION_ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY, List.of()));

        assertThat(authentication)
            .asInstanceOf(type(AbstractApiKeyAuthenticationToken.class))
            .satisfies(token -> assertThat(token.fetchEnvironmentId()).contains(PRODUCTION_ENVIRONMENT_ID));
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
                new EmbeddedMcpServerOAuth2AuthenticationToken(
                    ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY, List.of())));
    }

    @Test
    void testAuthenticateReturnsAnonymousWhenAuthenticationNotRequired() {
        mockMcpServer(false);

        Authentication authentication = provider.authenticate(
            new EmbeddedMcpServerOAuth2AuthenticationToken(
                ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY, List.of()));

        assertThat(authentication).isInstanceOf(McpAnonymousAuthenticationToken.class);
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(MCP_SERVER_SECRET_KEY);
        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void testSupportsOnlyOAuth2Token() {
        assertThat(provider.supports(EmbeddedMcpServerOAuth2AuthenticationToken.class)).isTrue();
        assertThat(provider.supports(EmbeddedMcpServerApiKeyAuthenticationToken.class)).isFalse();
    }

    private void mockMcpServer(boolean authenticationRequired) {
        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.isAuthenticationRequired()).thenReturn(authenticationRequired);
        when(mcpServerService.getMcpServer(MCP_SERVER_SECRET_KEY)).thenReturn(mcpServer);
    }
}

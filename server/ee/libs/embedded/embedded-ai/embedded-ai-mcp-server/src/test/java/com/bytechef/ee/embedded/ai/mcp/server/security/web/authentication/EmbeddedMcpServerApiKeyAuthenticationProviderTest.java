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
    private static final long PRODUCTION_ENVIRONMENT_ID = 2L;
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

    /**
     * Ticket 1051: the AUTHENTICATED token is what lands in the SecurityContext, and
     * {@code ConnectedUserResourceMembershipResolver} decides from it both whether the caller is a connected user and
     * WHICH environment to resolve its memberships in. Built with the {@code User}-only constructor this token carried
     * no environment, so {@code getEnvironmentId()} answered 0 -- a valid ordinal (DEVELOPMENT), not an obviously
     * absent one -- and the resolver would have gone looking for this caller's memberships in the wrong environment.
     * Latent only because MCP gates are {@code isTenantAdmin()} today.
     *
     * <p>
     * Deliberately uses a NON-ZERO environment: with ENVIRONMENT_ID the fabricated value and the real one coincide and
     * the bug is invisible.
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
            new EmbeddedMcpServerApiKeyAuthenticationToken(
                PRODUCTION_ENVIRONMENT_ID, EXTERNAL_USER_ID, "public", MCP_SERVER_SECRET_KEY));

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

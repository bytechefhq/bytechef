/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpTrustedIssuerResolverTest {

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);

    @Test
    void testResolvesEmbeddedFlaggedIssuersPlusEmbeddedAuthorizationServer() {
        IdentityProvider embeddedIdentityProvider = mock(IdentityProvider.class);

        when(embeddedIdentityProvider.isEnabled()).thenReturn(true);
        when(embeddedIdentityProvider.isMcpEmbedded()).thenReturn(true);
        when(embeddedIdentityProvider.getIssuerUri()).thenReturn("https://idp.customer.test");

        IdentityProvider disabledIdentityProvider = mock(IdentityProvider.class);

        when(disabledIdentityProvider.isEnabled()).thenReturn(false);

        IdentityProvider notEmbeddedFlaggedIdentityProvider = mock(IdentityProvider.class);

        when(notEmbeddedFlaggedIdentityProvider.isEnabled()).thenReturn(true);
        when(notEmbeddedFlaggedIdentityProvider.isMcpEmbedded()).thenReturn(false);
        when(notEmbeddedFlaggedIdentityProvider.getIssuerUri()).thenReturn("https://other.idp.test");

        IdentityProvider blankIssuerIdentityProvider = mock(IdentityProvider.class);

        when(blankIssuerIdentityProvider.isEnabled()).thenReturn(true);
        when(blankIssuerIdentityProvider.isMcpEmbedded()).thenReturn(true);
        when(blankIssuerIdentityProvider.getIssuerUri()).thenReturn("  ");

        when(identityProviderService.getIdentityProviders())
            .thenReturn(
                List.of(
                    embeddedIdentityProvider, disabledIdentityProvider, notEmbeddedFlaggedIdentityProvider,
                    blankIssuerIdentityProvider));

        EmbeddedMcpTrustedIssuerResolver embeddedMcpTrustedIssuerResolver =
            new EmbeddedMcpTrustedIssuerResolver(new McpTenantIssuerResolver(identityProviderService),
                "https://app.bytechef.test");

        assertThat(embeddedMcpTrustedIssuerResolver.resolveTrustedIssuerUris())
            .containsExactlyInAnyOrder("https://idp.customer.test", "https://app.bytechef.test");
    }

    @Test
    void testBlankEmbeddedAuthorizationServerIssuerIsExcluded() {
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of());

        EmbeddedMcpTrustedIssuerResolver embeddedMcpTrustedIssuerResolver =
            new EmbeddedMcpTrustedIssuerResolver(new McpTenantIssuerResolver(identityProviderService), "");

        assertThat(embeddedMcpTrustedIssuerResolver.resolveTrustedIssuerUris()).isEmpty();
    }
}

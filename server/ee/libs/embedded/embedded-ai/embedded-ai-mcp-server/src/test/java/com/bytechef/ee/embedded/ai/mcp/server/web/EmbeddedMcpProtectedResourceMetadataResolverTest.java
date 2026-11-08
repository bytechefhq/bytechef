/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpProtectedResourceMetadata;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpProtectedResourceMetadataResolverTest {

    private static final String RESOURCE = "https://app.bytechef.test/api/embedded/" + TenantKey.of("acme") + "/mcp";

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final AtomicBoolean secretVerified = new AtomicBoolean(true);
    private final EmbeddedMcpProtectedResourceMetadataResolver resolver =
        new EmbeddedMcpProtectedResourceMetadataResolver(
            new McpTenantIssuerResolver(identityProviderService),
            new McpTenantSecretVerifier(() -> List.of(request -> Optional.of(secretVerified.get()))));

    @Test
    void testAdvertisesAllTenantEmbeddedIdentityProvidersAndTheEmbeddedScope() {
        IdentityProvider identityProviderA = embeddedIdentityProvider("https://idp-a.test");
        IdentityProvider identityProviderB = embeddedIdentityProvider("https://idp-b.test");

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProviderA, identityProviderB));

        Optional<McpProtectedResourceMetadata> metadata = resolver.resolveMetadata(
            RESOURCE, new MockHttpServletRequest());

        assertThat(metadata).isPresent();

        McpProtectedResourceMetadata mcpProtectedResourceMetadata = metadata.get();

        assertThat(mcpProtectedResourceMetadata.resource()).isEqualTo(RESOURCE);
        assertThat(mcpProtectedResourceMetadata.authorizationServers())
            .containsExactlyInAnyOrder("https://idp-a.test", "https://idp-b.test");
        assertThat(mcpProtectedResourceMetadata.scopesSupported()).containsExactly("mcp:embedded");
    }

    @Test
    void testReturnsEmptyWithoutIdentityProviderLookupForAnUnverifiedSecret() {
        secretVerified.set(false);

        assertThat(resolver.resolveMetadata(RESOURCE, new MockHttpServletRequest())).isEmpty();

        verify(identityProviderService, never()).getIdentityProviders();
    }

    @Test
    void testReturnsEmptyWhenNoFlaggedIdentityProvider() {
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of());

        assertThat(resolver.resolveMetadata(RESOURCE, new MockHttpServletRequest())).isEmpty();
    }

    @Test
    void testReturnsEmptyForNonEmbeddedResource() {
        assertThat(
            resolver.resolveMetadata(
                "https://app.bytechef.test/api/automation/secret/mcp", new MockHttpServletRequest()))
                    .isEmpty();
    }

    private static IdentityProvider embeddedIdentityProvider(String issuerUri) {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(issuerUri);

        return identityProvider;
    }
}

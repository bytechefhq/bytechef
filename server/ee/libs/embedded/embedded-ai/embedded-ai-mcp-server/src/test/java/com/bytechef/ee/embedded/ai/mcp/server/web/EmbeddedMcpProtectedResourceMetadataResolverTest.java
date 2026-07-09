/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpProtectedResourceMetadataResolverTest {

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final EmbeddedMcpProtectedResourceMetadataResolver resolver =
        new EmbeddedMcpProtectedResourceMetadataResolver(new McpTenantIssuerResolver(identityProviderService));

    @Test
    void testAdvertisesAllTenantEmbeddedIdentityProviders() {
        IdentityProvider identityProviderA = embeddedIdentityProvider("https://idp-a.test");
        IdentityProvider identityProviderB = embeddedIdentityProvider("https://idp-b.test");

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProviderA, identityProviderB));

        String resource = "https://app.bytechef.test/api/embedded/" + TenantKey.of("acme") + "/mcp";

        Optional<ProtectedResourceMetadata> metadata = resolver.resolveMetadata(resource);

        assertThat(metadata).isPresent();
        assertThat(metadata.get()
            .resource()).isEqualTo(resource);
        assertThat(metadata.get()
            .authorizationServers()).containsExactlyInAnyOrder("https://idp-a.test", "https://idp-b.test");
    }

    @Test
    void testReturnsEmptyWhenNoFlaggedIdentityProvider() {
        when(identityProviderService.getIdentityProviders()).thenReturn(List.of());

        String resource = "https://app.bytechef.test/api/embedded/" + TenantKey.of("acme") + "/mcp";

        assertThat(resolver.resolveMetadata(resource)).isEmpty();
    }

    @Test
    void testReturnsEmptyForNonEmbeddedResource() {
        assertThat(resolver.resolveMetadata("https://app.bytechef.test/api/automation/secret/mcp")).isEmpty();
    }

    private static IdentityProvider embeddedIdentityProvider(String issuerUri) {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpEmbedded()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn(issuerUri);

        return identityProvider;
    }
}

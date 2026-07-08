/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpTenantProtectedResourceMetadataResolverTest {

    private static final String SECRET = String.valueOf(TenantKey.of("public"));

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final McpTenantProtectedResourceMetadataResolver resolver = new McpTenantProtectedResourceMetadataResolver(
        mcpResourceServerProperties(), new McpTenantIssuerResolver(identityProviderService));

    @Test
    void testAdvertisesTenantIdentityProvidersAheadOfStaticIssuers() {
        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.isEnabled()).thenReturn(true);
        when(identityProvider.isMcpAutomation()).thenReturn(true);
        when(identityProvider.getIssuerUri()).thenReturn("https://idp.customer.test");

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(identityProvider));

        String resource = "https://app.bytechef.test/api/automation/" + SECRET + "/mcp";

        Optional<McpProtectedResourceMetadata> metadata = resolver.resolveMetadata(resource);

        assertThat(metadata).isPresent();
        assertThat(metadata.get()
            .resource()).isEqualTo(resource);
        assertThat(metadata.get()
            .authorizationServers())
                .containsExactly("https://idp.customer.test", "https://as.bytechef.test");
    }

    @Test
    void testUsesTheSurfaceFlagSoAManagementProviderIsNotAdvertisedForAutomation() {
        IdentityProvider managementIdentityProvider = mock(IdentityProvider.class);

        when(managementIdentityProvider.isEnabled()).thenReturn(true);
        when(managementIdentityProvider.isMcpAutomation()).thenReturn(false);
        when(managementIdentityProvider.getIssuerUri()).thenReturn("https://idp.customer.test");

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(managementIdentityProvider));

        Optional<McpProtectedResourceMetadata> metadata = resolver.resolveMetadata(
            "https://app.bytechef.test/api/automation/" + SECRET + "/mcp");

        assertThat(metadata).isPresent();
        assertThat(metadata.get()
            .authorizationServers()).containsExactly("https://as.bytechef.test");
    }

    @Test
    void testReturnsEmptyForANonMcpResource() {
        assertThat(resolver.resolveMetadata("https://app.bytechef.test/api/other")).isEmpty();
    }

    private static McpResourceServerProperties mcpResourceServerProperties() {
        Issuer issuer = new Issuer();

        issuer.setUri("https://as.bytechef.test");

        McpResourceServerProperties mcpResourceServerProperties = new McpResourceServerProperties();

        mcpResourceServerProperties.setIssuers(List.of(issuer));

        return mcpResourceServerProperties;
    }
}

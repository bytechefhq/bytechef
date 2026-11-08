/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver.Surface;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpTenantIssuerResolverTest {

    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final McpTenantIssuerResolver mcpTenantIssuerResolver =
        new McpTenantIssuerResolver(identityProviderService);

    @Test
    void testResolvesOnlyEnabledProvidersApplicableToTheSurface() {
        IdentityProvider automation = identityProvider("https://automation.idp", true, ip -> {
            ip.setMcpAutomation(true);
            ip.setAuthorityMappings(Map.of("sales", "ROLE_SALES"));
        });
        IdentityProvider management = identityProvider("https://management.idp", true, ip -> ip.setMcpManagement(true));
        IdentityProvider disabledAutomation = identityProvider("https://disabled.idp", false,
            ip -> ip.setMcpAutomation(true));
        IdentityProvider embeddedOnly = identityProvider("https://embedded.idp", true, ip -> ip.setMcpEmbedded(true));

        when(identityProviderService.getIdentityProviders())
            .thenReturn(List.of(automation, management, disabledAutomation, embeddedOnly));

        List<McpTenantIssuer> resolved = mcpTenantIssuerResolver.resolve(Surface.AUTOMATION);

        assertThat(resolved).singleElement()
            .satisfies(issuer -> {
                assertThat(issuer.issuerUri()).isEqualTo("https://automation.idp");
                assertThat(issuer.authorityMappings())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("sales", "ROLE_SALES"));
            });
    }

    @Test
    void testCachesResolutionWithinTtl() {
        IdentityProvider automation = identityProvider("https://automation.idp", true,
            ip -> ip.setMcpAutomation(true));

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(automation));

        mcpTenantIssuerResolver.resolve(Surface.AUTOMATION);
        mcpTenantIssuerResolver.resolve(Surface.AUTOMATION);

        verify(identityProviderService, times(1)).getIdentityProviders();
    }

    @Test
    void testSkipsProvidersWithoutIssuerUri() {
        IdentityProvider noIssuer = identityProvider(null, true, ip -> ip.setMcpAutomation(true));

        when(identityProviderService.getIdentityProviders()).thenReturn(List.of(noIssuer));

        assertThat(mcpTenantIssuerResolver.resolve(Surface.AUTOMATION)).isEmpty();
    }

    private static IdentityProvider identityProvider(
        String issuerUri, boolean enabled, java.util.function.Consumer<IdentityProvider> customizer) {

        IdentityProvider identityProvider = mock(IdentityProvider.class);

        when(identityProvider.getIssuerUri()).thenReturn(issuerUri);
        when(identityProvider.isEnabled()).thenReturn(enabled);

        IdentityProvider realIdentityProvider = new IdentityProvider();

        customizer.accept(realIdentityProvider);

        when(identityProvider.isMcpEmbedded()).thenReturn(realIdentityProvider.isMcpEmbedded());
        when(identityProvider.isMcpAutomation()).thenReturn(realIdentityProvider.isMcpAutomation());
        when(identityProvider.isMcpManagement()).thenReturn(realIdentityProvider.isMcpManagement());
        when(identityProvider.isValidateMcpAudience()).thenReturn(realIdentityProvider.isValidateMcpAudience());
        when(identityProvider.getAuthorityMappings()).thenReturn(realIdentityProvider.getAuthorityMappings());

        return identityProvider;
    }
}

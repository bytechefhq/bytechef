/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.TenantIdpFederatedIssuerAuthenticator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.mcp.oauth2.McpAudienceValidator;
import com.bytechef.platform.security.web.mcp.oauth2.McpFederatedIssuerAuthenticator;
import com.bytechef.platform.security.web.mcp.oauth2.McpJwtIdentityMapper;
import com.bytechef.platform.user.service.IdentityProviderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the single, shared {@link McpTenantIssuerResolver} bean used by the automation/management trust filter and
 * discovery, and by the embedded MCP trust and discovery - so its per-tenant issuer cache is shared rather than
 * duplicated per call site. {@link McpTenantIssuerCacheEvictionListener} evicts that cache when any identity provider
 * changes, so trust and discovery reflect the change immediately instead of after the cache TTL.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class McpTenantIssuerResolverConfiguration {

    @Bean
    McpTenantIssuerResolver mcpTenantIssuerResolver(IdentityProviderService identityProviderService) {
        return new McpTenantIssuerResolver(identityProviderService);
    }

    /**
     * The enterprise implementation of the CE {@link McpFederatedIssuerAuthenticator} SPI. Present only on an EE build,
     * so the base JWT filter authenticates a non-statically-configured (per-tenant external IdP) issuer through
     * federation; on a CE-only build the SPI bean is absent and such an issuer is rejected.
     */
    @Bean
    McpFederatedIssuerAuthenticator mcpFederatedIssuerAuthenticator() {
        return new TenantIdpFederatedIssuerAuthenticator(new McpAudienceValidator(), new McpJwtIdentityMapper());
    }
}

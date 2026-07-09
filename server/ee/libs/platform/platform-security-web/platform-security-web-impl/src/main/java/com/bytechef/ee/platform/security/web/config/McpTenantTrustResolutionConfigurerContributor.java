/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantTrustResolutionFilter;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.DisabledMcpOAuth2ResourceServerConfigurer;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Enterprise-only contributor that adds the per-tenant external-IdP trust filter to the MCP endpoints' chain. Active
 * only in EE and only when at least one trusted issuer is configured; otherwise a no-op. Complements the CE
 * {@code McpJwtSecurityConfigurerContributor} (which wires the base post-decode policy filter) and the EE
 * {@code McpFederatedIssuerAuthenticator} bean (which the base filter delegates non-static issuers to).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class McpTenantTrustResolutionConfigurerContributor implements SecurityConfigurerContributor {

    private final McpResourceServerProperties mcpResourceServerProperties;
    private final McpTenantIssuerResolver mcpTenantIssuerResolver;

    @SuppressFBWarnings("EI2")
    public McpTenantTrustResolutionConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, McpTenantIssuerResolver mcpTenantIssuerResolver) {

        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        if (mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        return (T) new McpTenantTrustResolutionConfigurer(
            new McpTenantTrustResolutionFilter(mcpTenantIssuerResolver));
    }
}

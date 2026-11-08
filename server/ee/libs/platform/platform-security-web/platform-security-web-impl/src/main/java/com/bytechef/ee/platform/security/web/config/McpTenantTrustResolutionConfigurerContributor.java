/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
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
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class McpTenantTrustResolutionConfigurerContributor implements SecurityConfigurerContributor {

    private final McpResourceServerProperties mcpResourceServerProperties;
    private final McpTenantIssuerResolver mcpTenantIssuerResolver;
    private final McpTenantSecretVerifier mcpTenantSecretVerifier;

    @SuppressFBWarnings("EI2")
    public McpTenantTrustResolutionConfigurerContributor(
        McpResourceServerProperties mcpResourceServerProperties, McpTenantIssuerResolver mcpTenantIssuerResolver,
        McpTenantSecretVerifier mcpTenantSecretVerifier) {

        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
        this.mcpTenantSecretVerifier = mcpTenantSecretVerifier;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T getSecurityConfigurerAdapter() {
        if (mcpResourceServerProperties.getIssuers()
            .isEmpty()) {

            return (T) new DisabledMcpOAuth2ResourceServerConfigurer();
        }

        return (T) new McpTenantTrustResolutionConfigurer(
            new McpTenantTrustResolutionFilter(mcpTenantIssuerResolver, mcpTenantSecretVerifier));
    }
}

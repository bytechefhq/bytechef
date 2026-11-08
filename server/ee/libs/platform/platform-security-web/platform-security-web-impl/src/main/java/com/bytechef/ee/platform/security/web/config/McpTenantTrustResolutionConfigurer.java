/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantTrustResolutionFilter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class McpTenantTrustResolutionConfigurer
    extends AbstractHttpConfigurer<McpTenantTrustResolutionConfigurer, HttpSecurity> {

    private final McpTenantTrustResolutionFilter mcpTenantTrustResolutionFilter;

    @SuppressFBWarnings("EI2")
    public McpTenantTrustResolutionConfigurer(McpTenantTrustResolutionFilter mcpTenantTrustResolutionFilter) {
        this.mcpTenantTrustResolutionFilter = mcpTenantTrustResolutionFilter;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilterBefore(mcpTenantTrustResolutionFilter, BearerTokenAuthenticationFilter.class);
    }
}

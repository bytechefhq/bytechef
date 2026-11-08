/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.config;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantProtectedResourceMetadataResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnProperty(name = "bytechef.oauth2.resource-server.issuers[0].uri")
public class McpTenantDiscoverySecurityConfiguration {

    @Bean
    McpTenantProtectedResourceMetadataResolver mcpTenantProtectedResourceMetadataResolver(
        McpResourceServerProperties mcpResourceServerProperties, McpTenantIssuerResolver mcpTenantIssuerResolver,
        McpTenantSecretVerifier mcpTenantSecretVerifier) {

        return new McpTenantProtectedResourceMetadataResolver(
            mcpResourceServerProperties, mcpTenantIssuerResolver, mcpTenantSecretVerifier);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 15)
    SecurityFilterChain mcpTenantProtectedResourceMetadataFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(
                "/.well-known/oauth-protected-resource/api/automation/**",
                "/.well-known/oauth-protected-resource/api/management/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web.config;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.ai.mcp.server.web.EmbeddedMcpProtectedResourceMetadataResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class EmbeddedMcpDiscoveryConfiguration {

    @Bean
    EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver(
        McpTenantIssuerResolver mcpTenantIssuerResolver, McpTenantSecretVerifier mcpTenantSecretVerifier) {

        return new EmbeddedMcpProtectedResourceMetadataResolver(mcpTenantIssuerResolver, mcpTenantSecretVerifier);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 14)
    SecurityFilterChain embeddedMcpProtectedResourceMetadataFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/.well-known/oauth-protected-resource/api/embedded/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }
}

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
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Wires the embedded MCP protected-resource metadata endpoint. The metadata lives at
 * {@code /.well-known/oauth-protected-resource/api/embedded/**}, which the application's {@code /api/**} chain does not
 * match, so a dedicated high-precedence permit-all chain serves it - the "your IdP runs the show" discovery surface.
 *
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
        McpTenantIssuerResolver mcpTenantIssuerResolver) {

        return new EmbeddedMcpProtectedResourceMetadataResolver(mcpTenantIssuerResolver);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 30)
    SecurityFilterChain embeddedMcpProtectedResourceMetadataFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/.well-known/oauth-protected-resource/api/embedded/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }
}

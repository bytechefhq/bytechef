/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web.config;

import static org.mockito.Mockito.mock;

import com.bytechef.ee.embedded.ai.mcp.server.web.EmbeddedMcpProtectedResourceMetadataController;
import com.bytechef.ee.embedded.ai.mcp.server.web.EmbeddedMcpProtectedResourceMetadataResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Boots a live Tomcat context (no database) that serves the embedded MCP protected-resource metadata endpoint with a
 * mocked {@link IdentityProviderService}, so the test controls the tenant's flagged identity provider.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration(excludeName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
    "org.springframework.boot.data.jdbc.autoconfigure.JdbcRepositoriesAutoConfiguration",
    "org.springframework.boot.sql.init.autoconfigure.SqlInitializationAutoConfiguration"
})
@EnableWebSecurity
@Configuration
public class EmbeddedMcpDiscoveryIntTestConfiguration {

    @Bean
    IdentityProviderService identityProviderService() {
        return mock(IdentityProviderService.class);
    }

    @Bean
    EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver(
        IdentityProviderService identityProviderService) {

        return new EmbeddedMcpProtectedResourceMetadataResolver(
            new McpTenantIssuerResolver(identityProviderService));
    }

    @Bean
    EmbeddedMcpProtectedResourceMetadataController embeddedMcpProtectedResourceMetadataController(
        EmbeddedMcpProtectedResourceMetadataResolver embeddedMcpProtectedResourceMetadataResolver) {

        return new EmbeddedMcpProtectedResourceMetadataController(embeddedMcpProtectedResourceMetadataResolver);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/.well-known/oauth-protected-resource/api/embedded/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }
}

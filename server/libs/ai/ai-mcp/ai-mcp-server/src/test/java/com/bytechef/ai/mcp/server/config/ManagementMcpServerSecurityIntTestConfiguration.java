/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.server.config;

import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.security.web.configurer.ManagementMcpServerSecurityConfigurer;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Boots a live Tomcat + PostgreSQL context that exercises the real management MCP security filter chain against the
 * real streamable-HTTP MCP transport. The DB-backed {@link ApiKeyService} is provided by the imported
 * {@code platform-security-service} module; {@link UserService}, {@link AuthorityService} and {@link PropertyService}
 * are stubbed so the test controls the user context and configured MCP server secret.
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@EnableAutoConfiguration
@EnableWebSecurity
@Import(LiquibaseConfiguration.class)
@Configuration
public class ManagementMcpServerSecurityIntTestConfiguration {

    private static final String MCP_ENDPOINT = "/api/management/{secretKey}/mcp";

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    PropertyService propertyService() {
        return mock(PropertyService.class);
    }

    @Bean
    UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(MCP_ENDPOINT)
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> mcpRouterFunction(WebMvcStreamableServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return McpServer.async(transportProvider)
            .serverInfo("mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build())
            .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
        PropertyService propertyService, UserService userService) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .with(
                new ManagementMcpServerSecurityConfigurer(
                    apiKeyService, authorityService, propertyService, userService),
                Customizer.withDefaults())
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class ManagementMcpServerSecurityIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}

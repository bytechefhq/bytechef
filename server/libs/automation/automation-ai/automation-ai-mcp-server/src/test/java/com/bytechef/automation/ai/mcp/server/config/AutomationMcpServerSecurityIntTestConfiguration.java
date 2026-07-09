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

package com.bytechef.automation.ai.mcp.server.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.ai.mcp.server.security.web.configurer.AutomationMcpServerSecurityConfigurer;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
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
 * Boots a live Tomcat + PostgreSQL context that exercises the real automation MCP security filter chain against the
 * real streamable-HTTP MCP transport. The DB-backed {@link ApiKeyService} is provided by the imported
 * {@code platform-security-service} module; {@link UserService}, {@link AuthorityService} and {@link McpServerService}
 * are stubbed so the test controls the user/server context.
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@EnableAutoConfiguration
@EnableWebSecurity
@Import(LiquibaseConfiguration.class)
@Configuration
public class AutomationMcpServerSecurityIntTestConfiguration {

    private static final String MCP_ENDPOINT = "/api/automation/{secretKey}/mcp";
    private static final String SECRET_KEY = "secretKey";

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    McpServerService mcpServerService() {
        return mock(McpServerService.class);
    }

    @Bean
    UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    WebMvcStreamableServerTransportProvider automationWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint(MCP_ENDPOINT)
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);

                return McpTransportContext.create(Map.of(SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> automationMcpRouterFunction(
        WebMvcStreamableServerTransportProvider transportProvider) {

        return transportProvider.getRouterFunction();
    }

    @Bean
    Object automationMcpAsyncServer(WebMvcStreamableServerTransportProvider transportProvider) {
        return new FilterableMcpServerBuilder(transportProvider)
            .serverInfo("automation-mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .tools(true)
                    .build())
            .toolFilter(exchange -> List.of())
            .build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
        McpServerService mcpServerService, UserService userService) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .with(
                new AutomationMcpServerSecurityConfigurer(
                    apiKeyService, authorityService, mcpServerService, userService),
                Customizer.withDefaults())
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationMcpServerSecurityIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}

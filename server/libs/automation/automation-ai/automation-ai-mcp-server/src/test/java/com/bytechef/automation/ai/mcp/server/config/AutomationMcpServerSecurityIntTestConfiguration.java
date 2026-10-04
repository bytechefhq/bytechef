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
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
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

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@EnableAutoConfiguration
@EnableWebSecurity
@Import({
    AutomationMcpServerConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AutomationMcpServerSecurityIntTestConfiguration {

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    McpComponentService mcpComponentService() {
        return mock(McpComponentService.class);
    }

    @Bean
    McpProjectService mcpProjectService() {
        return mock(McpProjectService.class);
    }

    @Bean
    McpServerService mcpServerService() {
        return mock(McpServerService.class);
    }

    @Bean
    McpToolService mcpToolService() {
        return mock(McpToolService.class);
    }

    @Bean
    UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    WorkspaceMcpServerService workspaceMcpServerService() {
        return mock(WorkspaceMcpServerService.class);
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

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

package com.bytechef.automation.ai.a2a.server.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.ai.a2a.server.security.web.configurer.AutomationA2AServerSecurityConfigurer;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.a2a.A2AAgentCardFactory;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.security")
@ComponentScan(
    basePackages = "com.bytechef.automation.ai.a2a.server.web.rest", useDefaultFilters = false,
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = RestController.class))
@EnableAutoConfiguration
@EnableWebSecurity
@Import(LiquibaseConfiguration.class)
@Configuration
public class AutomationA2AServerSecurityIntTestConfiguration {

    @Bean
    A2AAgentCardFactory a2aAgentCardFactory() {
        return new A2AAgentCardFactory();
    }

    @Bean
    A2aServerService a2aServerService() {
        return mock(A2aServerService.class);
    }

    @Bean
    AuthorityService authorityService() {
        return mock(AuthorityService.class);
    }

    @Bean
    UserService userService() {
        return mock(UserService.class);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http, ApiKeyService apiKeyService, AuthorityService authorityService,
        A2aServerService a2aServerService, UserService userService) throws Exception {

        return http
            .authorizeHttpRequests(authorize -> authorize.anyRequest()
                .permitAll())
            .with(
                new AutomationA2AServerSecurityConfigurer(
                    apiKeyService, authorityService, a2aServerService, userService),
                Customizer.withDefaults())
            .build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AutomationA2AServerSecurityIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}

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

package com.bytechef.platform.oauth2.authorizationserver.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Boots a live Tomcat + PostgreSQL context that exercises the embedded OAuth2 authorization server. The authorization
 * server beans are conditionally activated by the {@code bytechef.oauth2.authorization-server.enabled} property, which
 * each test toggles independently.
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration(exclude = GraphQlAutoConfiguration.class)
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableWebSecurity
@Import({
    LiquibaseConfiguration.class, Oauth2AuthorizationServerConfiguration.class,
    Oauth2AuthorizationServerEncryptionIntTestConfiguration.class
})
@Configuration
@SuppressFBWarnings("SPRING_CSRF_PROTECTION_DISABLED")
public class Oauth2AuthorizationServerIntTestConfiguration {

    /**
     * Permit-all security chain used only when the authorization server is disabled, so that a request to an absent
     * authorization server endpoint resolves to a genuine 404 rather than a 401 from Spring Boot's default security,
     * with CSRF off so that a POST to one is not rejected with a 403 before it reaches the dispatcher. The production
     * module does not register this chain; the surrounding application supplies its own security when the authorization
     * server is off.
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnProperty(
        name = "bytechef.oauth2.authorization-server.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest()
            .permitAll())
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class Oauth2AuthorizationServerIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}

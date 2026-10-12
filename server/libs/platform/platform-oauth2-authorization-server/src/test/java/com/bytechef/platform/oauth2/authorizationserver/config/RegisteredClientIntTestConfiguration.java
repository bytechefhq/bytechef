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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Boots a PostgreSQL-backed context with the authorization server persistence beans and the registered-client and grant
 * services.
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.oauth2.authorizationserver.service")
@EnableAutoConfiguration(exclude = GraphQlAutoConfiguration.class)
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableWebSecurity
@Import({
    LiquibaseConfiguration.class, Oauth2AuthorizationServerConfiguration.class,
    Oauth2AuthorizationServerEncryptionIntTestConfiguration.class
})
@Configuration
public class RegisteredClientIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class RegisteredClientIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}

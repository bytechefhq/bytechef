/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.user.web.rest.config;

import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.jdbc.config.AuditingJdbcConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.security.config.SecurityConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = {
    "com.bytechef.platform.user", "com.bytechef.platform.web.rest", "com.bytechef.tenant", "com.bytechef.ee.tenant"
})
@EnableAutoConfiguration
@Import({
    AuditingJdbcConfiguration.class, CacheConfiguration.class, LiquibaseConfiguration.class,
    PostgreSQLContainerConfiguration.class, SecurityConfiguration.class
})
@Configuration
public class UserIntTestConfiguration {

    @MockBean
    private AuthenticationFailureHandler authenticationFailureHandler;

    @MockBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockBean
    private RememberMeServices rememberMeServices;

    @Bean
    JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }
}

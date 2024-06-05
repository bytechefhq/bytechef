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

package com.bytechef.platform.user.config;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ComponentScan(basePackages = {
    "com.bytechef.platform.user", "com.bytechef.liquibase.config"
})
@EnableAutoConfiguration
@EnableCaching
@Import(PostgreSQLContainerConfiguration.class)
@Configuration
public class UserIntTestConfiguration {

    @Bean
    JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @EnableJdbcAuditing
    @EnableJdbcRepositories(
        basePackages = {
            "com.bytechef.platform.user.repository"
        })
    public static class UserIntTestJdbcConfiguration extends AbstractJdbcConfiguration {

        @Bean
        public DateTimeProvider auditingDateTimeProvider() {
            return CurrentDateTimeProvider.INSTANCE;
        }
    }
}

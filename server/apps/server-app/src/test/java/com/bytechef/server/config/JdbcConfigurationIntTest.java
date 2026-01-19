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

package com.bytechef.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.tenant.multi.sql.MultiTenantDataSource;
import com.bytechef.encryption.Encryption;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@link JdbcConfiguration} to verify that the correct DataSource is loaded based on tenant mode
 * configuration.
 *
 * <p>
 * These tests verify that when tenant mode is "single" (explicitly set) or not set (matchIfMissing = true), the
 * JdbcConfiguration DataSource bean is active and returns a HikariDataSource (not wrapped in MultiTenantDataSource).
 *
 * @author Ivica Cardic
 */
@Testcontainers
class JdbcConfigurationIntTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
        .withUserConfiguration(JdbcConfiguration.class, TestConfiguration.class)
        .withPropertyValues(
            "bytechef.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
            "bytechef.datasource.username=" + postgreSQLContainer.getUsername(),
            "bytechef.datasource.password=" + postgreSQLContainer.getPassword());

    @Test
    void testDataSourceIsHikariDataSourceInSingleTenantMode() {
        contextRunner.withPropertyValues("bytechef.tenant.mode=single")
            .run(context -> {
                assertThat(context).hasSingleBean(DataSource.class);

                DataSource dataSource = context.getBean(DataSource.class);

                assertThat(dataSource).isInstanceOf(HikariDataSource.class);
                assertThat(dataSource).isNotInstanceOf(MultiTenantDataSource.class);
            });
    }

    @Test
    void testDataSourceIsHikariDataSourceWhenTenantModeNotSet() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DataSource.class);

            DataSource dataSource = context.getBean(DataSource.class);

            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
            assertThat(dataSource).isNotInstanceOf(MultiTenantDataSource.class);
        });
    }

    @Test
    void testDataSourceConnectionIsValid() {
        contextRunner.withPropertyValues("bytechef.tenant.mode=single")
            .run(context -> {
                DataSource dataSource = context.getBean(DataSource.class);

                try (var connection = dataSource.getConnection()) {
                    assertThat(connection).isNotNull();
                    assertThat(connection.isValid(5)).isTrue();
                }
            });
    }

    @Configuration
    @EnableConfigurationProperties(ApplicationProperties.class)
    static class TestConfiguration {

        @Bean
        Encryption encryption() {
            return mock(Encryption.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}

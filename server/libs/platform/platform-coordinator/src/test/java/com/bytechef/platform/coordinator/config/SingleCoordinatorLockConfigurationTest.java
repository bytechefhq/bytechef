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

package com.bytechef.platform.coordinator.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.coordinator.lock.SingleCoordinatorLock;
import java.time.Duration;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
class SingleCoordinatorLockConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withUserConfiguration(SingleCoordinatorLockConfiguration.class, DataSourcePropertiesConfiguration.class)
        .withBean(ApplicationProperties.class, SingleCoordinatorLockConfigurationTest::applicationProperties)
        .withBean(JdbcConnectionDetails.class, SingleCoordinatorLockConfigurationTest::h2ConnectionDetails);

    @Test
    void testRegisteredOnCeCoordinator() {
        applicationContextRunner.withPropertyValues("bytechef.edition=ce")
            .run(context -> assertThat(context).hasSingleBean(SingleCoordinatorLock.class));
    }

    @Test
    void testAbsentInEe() {
        applicationContextRunner.withPropertyValues("bytechef.edition=ee")
            .run(context -> assertThat(context).doesNotHaveBean(SingleCoordinatorLock.class));
    }

    @Test
    void testAbsentWhenCoordinatorIsDisabled() {
        applicationContextRunner.withPropertyValues("bytechef.edition=ce", "bytechef.coordinator.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(SingleCoordinatorLock.class));
    }

    @Test
    void testRegisteredWithoutJdbcConnectionDetailsWhenApplicationDefinesItsOwnDataSource() {
        new ApplicationContextRunner()
            .withUserConfiguration(SingleCoordinatorLockConfiguration.class, DataSourcePropertiesConfiguration.class)
            .withBean(ApplicationProperties.class, SingleCoordinatorLockConfigurationTest::applicationProperties)
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withPropertyValues("bytechef.edition=ce", "spring.datasource.url=jdbc:h2:mem:test")
            .run(context -> assertThat(context).hasSingleBean(SingleCoordinatorLock.class));
    }

    @Test
    void testLockConnectionPropertiesSetServerSideKeepalives() {
        Properties properties = SingleCoordinatorLockConfiguration.lockConnectionProperties("user", "secret");

        assertThat(properties.getProperty("options"))
            .isEqualTo("-c tcp_keepalives_idle=10 -c tcp_keepalives_interval=5 -c tcp_keepalives_count=3");
        assertThat(properties.getProperty("user")).isEqualTo("user");
        assertThat(properties.getProperty("password")).isEqualTo("secret");
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getCoordinator()
            .setLockWait(Duration.ofSeconds(60));

        return applicationProperties;
    }

    private static JdbcConnectionDetails h2ConnectionDetails() {
        JdbcConnectionDetails jdbcConnectionDetails = mock(JdbcConnectionDetails.class);

        when(jdbcConnectionDetails.getJdbcUrl()).thenReturn("jdbc:h2:mem:test");

        return jdbcConnectionDetails;
    }

    @Configuration
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class DataSourcePropertiesConfiguration {
    }
}

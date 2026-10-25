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

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.coordinator.lock.SingleCoordinatorLock;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnCEVersion
@ConditionalOnCoordinator
public class SingleCoordinatorLockConfiguration {

    private static final String SERVER_SIDE_KEEPALIVE_OPTIONS =
        "-c tcp_keepalives_idle=10 -c tcp_keepalives_interval=5 -c tcp_keepalives_count=3";
    private static final int SOCKET_TIMEOUT_SECONDS = 30;

    @Bean
    SingleCoordinatorLock singleCoordinatorLock(
        ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetailsProvider, DataSourceProperties dataSourceProperties,
        ConfigurableApplicationContext applicationContext, ApplicationProperties applicationProperties) {

        JdbcConnectionDetails jdbcConnectionDetails = jdbcConnectionDetailsProvider.getIfAvailable();

        String jdbcUrl;
        String username;
        String password;

        if (jdbcConnectionDetails == null) {
            jdbcUrl = dataSourceProperties.determineUrl();
            username = dataSourceProperties.determineUsername();
            password = dataSourceProperties.determinePassword();
        } else {
            jdbcUrl = jdbcConnectionDetails.getJdbcUrl();
            username = jdbcConnectionDetails.getUsername();
            password = jdbcConnectionDetails.getPassword();
        }

        Properties connectionProperties = lockConnectionProperties(username, password);

        Duration lockWait = applicationProperties.getCoordinator()
            .getLockWait();

        return new SingleCoordinatorLock(
            jdbcUrl, () -> DriverManager.getConnection(jdbcUrl, connectionProperties),
            () -> Thread.ofPlatform()
                .start(() -> System.exit(SpringApplication.exit(applicationContext, () -> 1))),
            lockWait == null ? Duration.ZERO : lockWait);
    }

    public static Properties lockConnectionProperties(String username, String password) {
        Properties connectionProperties = new Properties();

        if (username != null) {
            connectionProperties.setProperty("user", username);
        }

        if (password != null) {
            connectionProperties.setProperty("password", password);
        }

        connectionProperties.setProperty("options", SERVER_SIDE_KEEPALIVE_OPTIONS);
        connectionProperties.setProperty("socketTimeout", String.valueOf(SOCKET_TIMEOUT_SECONDS));

        return connectionProperties;
    }
}

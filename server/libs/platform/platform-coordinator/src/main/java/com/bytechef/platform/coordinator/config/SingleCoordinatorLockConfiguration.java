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
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.coordinator.lock.SingleCoordinatorLock;
import java.sql.DriverManager;
import org.springframework.boot.SpringApplication;
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

    @Bean
    SingleCoordinatorLock singleCoordinatorLock(
        JdbcConnectionDetails jdbcConnectionDetails, ConfigurableApplicationContext applicationContext) {

        String jdbcUrl = jdbcConnectionDetails.getJdbcUrl();

        return new SingleCoordinatorLock(
            jdbcUrl,
            () -> DriverManager.getConnection(
                jdbcUrl, jdbcConnectionDetails.getUsername(), jdbcConnectionDetails.getPassword()),
            () -> Thread.ofPlatform()
                .start(() -> System.exit(SpringApplication.exit(applicationContext, () -> 1))));
    }
}

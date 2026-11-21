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

package com.bytechef.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Ivica Cardic
 */
@Testcontainers
class LiquibaseMigrationRunnerIntTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @ParameterizedTest
    @ValueSource(strings = {
        "ce", "ee"
    })
    void testMigrationRunCompletesAndShutsDown(String edition) throws SQLException {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(
            ServerApplication.class, "--spring.profiles.active=liquibase,testint", "--bytechef.edition=" + edition,
            "--bytechef.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
            "--bytechef.datasource.username=" + postgreSQLContainer.getUsername(),
            "--bytechef.datasource.password=" + postgreSQLContainer.getPassword());

        assertThat(applicationContext.isActive()).isFalse();
        assertThat(countAppliedChangeSets()).isPositive();
    }

    private static int countAppliedChangeSets() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
            postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }

            return 0;
        }
    }
}

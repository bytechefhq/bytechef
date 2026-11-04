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

package com.bytechef.platform.data.table.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;

class DataTableWebhookWorkflowExecutionIdMigrationIntTest {

    private static final String CHANGELOG_PATH = "config/liquibase/changelog/platform/data_table/";
    private static final String H2 = "h2";
    private static final String POSTGRESQL = "postgresql";

    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>("postgres:16-alpine");

    private Connection connection;

    @BeforeAll
    static void beforeAll() {
        POSTGRESQL_CONTAINER.start();
    }

    @AfterAll
    static void afterAll() {
        POSTGRESQL_CONTAINER.stop();
    }

    @AfterEach
    void afterEach() throws SQLException {
        connection.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        H2, POSTGRESQL
    })
    void testUrlsAreReplacedByTheirWorkflowExecutionIdAndUnusableRowsAreDeleted(String database) throws Exception {
        connect(database);
        runChangelog(CHANGELOG_PATH + "00000000000001_platform_data_table_init.xml");

        String idWithSlash = "YWJj/ZGVm+Z2hp==";

        insertDataTable();
        insertWebhook(1L, "http://127.0.0.1:9555/webhooks/" + idWithSlash + "?token=1");
        insertWebhook(2L, "https://example.com/webhooks/dGVuYW50");
        insertWebhook(3L, "https://example.com/hooks/abc");
        insertWebhook(4L, "https://example.com/webhooks/");

        runChangelog(CHANGELOG_PATH + "20261103000001_platform_data_table_webhook_workflow_execution_id.xml");

        assertThat(selectWorkflowExecutionIdsById()).containsExactly(entry(1L, idWithSlash), entry(2L, "dGVuYW50"));
        assertThat(selectColumnNames()).contains("URL", "WORKFLOW_EXECUTION_ID");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        H2, POSTGRESQL
    })
    void testRegistrationsWithoutAUrlCanBeSavedAfterTheMigration(String database) throws Exception {
        connect(database);
        runChangelog(CHANGELOG_PATH + "00000000000001_platform_data_table_init.xml");

        insertDataTable();

        runChangelog(CHANGELOG_PATH + "20261103000001_platform_data_table_webhook_workflow_execution_id.xml");

        insertRegistration(1L, "dGVuYW50");

        assertThat(selectWorkflowExecutionIdsById()).containsExactly(entry(1L, "dGVuYW50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        H2, POSTGRESQL
    })
    void testDuplicateIdsKeepTheLatestRegistrationAndOverlongUrlsDoNotStopTheMigration(String database)
        throws Exception {
        connect(database);
        runChangelog(CHANGELOG_PATH + "00000000000001_platform_data_table_init.xml");

        insertDataTable();
        insertWebhook(1L, "http://old-host:9555/webhooks/dGVuYW50");
        insertWebhook(2L, "https://new-host/webhooks/dGVuYW50");
        insertWebhook(3L, "https://example.com/" + "a".repeat(600));

        runChangelog(CHANGELOG_PATH + "20261103000001_platform_data_table_webhook_workflow_execution_id.xml");

        assertThat(selectWorkflowExecutionIdsById()).containsExactly(entry(2L, "dGVuYW50"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        H2, POSTGRESQL
    })
    void testRegistrationsOfThePreviousVersionCanBeSavedAfterTheMigration(String database) throws Exception {
        connect(database);
        runChangelog(CHANGELOG_PATH + "00000000000001_platform_data_table_init.xml");

        insertDataTable();

        runChangelog(CHANGELOG_PATH + "20261103000001_platform_data_table_webhook_workflow_execution_id.xml");

        insertWebhook(1L, "https://example.com/webhooks/dGVuYW50");

        assertThat(selectWorkflowExecutionIdsById()).containsExactly(entry(1L, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        H2, POSTGRESQL
    })
    void testTheUrlColumnIsDroppedAfterTheWorkflowExecutionIdMigration(String database) throws Exception {
        connect(database);
        runChangelog(CHANGELOG_PATH + "00000000000001_platform_data_table_init.xml");

        insertDataTable();
        insertWebhook(1L, "https://example.com/webhooks/dGVuYW50");

        runChangelog(CHANGELOG_PATH + "20261103000001_platform_data_table_webhook_workflow_execution_id.xml");

        insertWebhook(2L, "https://example.com/webhooks/cm9sbGJhY2s=?token=1");
        insertWebhook(3L, "https://example.com/hooks/abc");
        insertWebhook(4L, "https://other-host/webhooks/dGVuYW50");

        runChangelog(CHANGELOG_PATH + "20261104000001_platform_data_table_webhook_drop_url.xml");

        assertThat(selectColumnNames()).doesNotContain("URL")
            .contains("WORKFLOW_EXECUTION_ID");
        assertThat(selectWorkflowExecutionIdsById()).containsExactly(entry(2L, "cm9sbGJhY2s="), entry(4L, "dGVuYW50"));
    }

    private void connect(String database) throws SQLException {
        if (POSTGRESQL.equals(database)) {
            connection = DriverManager.getConnection(
                POSTGRESQL_CONTAINER.getJdbcUrl(), POSTGRESQL_CONTAINER.getUsername(),
                POSTGRESQL_CONTAINER.getPassword());

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA public CASCADE");
                statement.execute("CREATE SCHEMA public");
            }
        } else {
            JdbcDataSource jdbcDataSource = new JdbcDataSource();

            jdbcDataSource.setURL(
                "jdbc:h2:mem:" + UUID.randomUUID() +
                    ";DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=KEY,VALUE,USER");

            connection = jdbcDataSource.getConnection();
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE tag (id BIGINT PRIMARY KEY)");
        }
    }

    private void insertDataTable() throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "INSERT INTO data_table (id, name, created_date, created_by, last_modified_date, last_modified_by, " +
                "version) VALUES (1, 'orders', ?, 'admin', ?, 'admin', 0)")) {

            Timestamp now = Timestamp.from(Instant.now());

            preparedStatement.setTimestamp(1, now);
            preparedStatement.setTimestamp(2, now);

            preparedStatement.executeUpdate();
        }
    }

    private void insertWebhook(long id, String url) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "INSERT INTO data_table_webhook (id, data_table_id, url, type, environment, created_date, created_by, " +
                "last_modified_date, last_modified_by, version) VALUES (?, 1, ?, 0, 0, ?, 'admin', ?, 'admin', 0)")) {

            Timestamp now = Timestamp.from(Instant.now());

            preparedStatement.setLong(1, id);
            preparedStatement.setString(2, url);
            preparedStatement.setTimestamp(3, now);
            preparedStatement.setTimestamp(4, now);

            preparedStatement.executeUpdate();
        }
    }

    private void insertRegistration(long id, String workflowExecutionId) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "INSERT INTO data_table_webhook (id, data_table_id, workflow_execution_id, type, environment, " +
                "created_date, created_by, last_modified_date, last_modified_by, version) " +
                "VALUES (?, 1, ?, 0, 0, ?, 'admin', ?, 'admin', 0)")) {

            Timestamp now = Timestamp.from(Instant.now());

            preparedStatement.setLong(1, id);
            preparedStatement.setString(2, workflowExecutionId);
            preparedStatement.setTimestamp(3, now);
            preparedStatement.setTimestamp(4, now);

            preparedStatement.executeUpdate();
        }
    }

    private void runChangelog(String changelogFile) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection));

        new CommandScope(UpdateCommandStep.COMMAND_NAME)
            .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
            .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changelogFile)
            .execute();
    }

    private List<String> selectColumnNames() throws SQLException {
        List<String> columnNames = new ArrayList<>();

        DatabaseMetaData databaseMetaData = connection.getMetaData();

        String tableName = databaseMetaData.storesLowerCaseIdentifiers() ? "data_table_webhook" : "DATA_TABLE_WEBHOOK";

        try (ResultSet resultSet = databaseMetaData.getColumns(null, null, tableName, null)) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");

                columnNames.add(columnName.toUpperCase(Locale.ROOT));
            }
        }

        return columnNames;
    }

    private Map<Long, String> selectWorkflowExecutionIdsById() throws SQLException {
        Map<Long, String> workflowExecutionIds = new LinkedHashMap<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(
            "SELECT id, workflow_execution_id FROM data_table_webhook ORDER BY id");
            ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                workflowExecutionIds.put(resultSet.getLong(1), resultSet.getString(2));
            }
        }

        return workflowExecutionIds;
    }
}

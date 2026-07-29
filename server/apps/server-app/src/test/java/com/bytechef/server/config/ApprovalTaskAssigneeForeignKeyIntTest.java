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
import static org.assertj.core.api.Assertions.assertThatCode;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Pins the split of {@code 20260425120000} into {@code -1} (columns and indexes) and {@code -2} (the assignee foreign
 * key).
 *
 * <p>
 * The split exists because the two halves belong to different deployments. {@code approval_task} is created under
 * {@code mono or configuration or multitenant} while {@code user} is created under {@code mono or user or multitenant},
 * so under the configuration context the table exists and the table it points at does not. While both halves lived in
 * one changeset the whole migration died there with {@code relation "public.user" does not exist}, taking
 * configuration-app's context down with it. Guarding the changeset instead of splitting it would have skipped the
 * columns and indexes that the configuration context does need.
 * </p>
 *
 * <p>
 * Each test covers a deployment the split has to keep working, and the middle two are the ones that would fail
 * silently. {@code -2} re-added to a database that already has the foreign key would abort an upgrade on a duplicate
 * constraint, and {@code -1} is a released changeset whose body moved, so every database that ran the unsplit version
 * carries a checksum that no longer matches what is on disk.
 * </p>
 *
 * @author Ivica Cardic
 */
@Testcontainers
class ApprovalTaskAssigneeForeignKeyIntTest {

    private static final String CHANGELOG =
        "config/liquibase/changelog/automation/task/20260425120000_automation_task_add_status_assignee_priority.xml";
    private static final String FOREIGN_KEY = "fk_approval_task_assignee";

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeEach
    void beforeEach() throws SQLException {
        execute(
            "DROP TABLE IF EXISTS approval_task", "DROP TABLE IF EXISTS \"user\"",
            "DROP TABLE IF EXISTS databasechangelog", "DROP TABLE IF EXISTS databasechangeloglock");
    }

    @Test
    void testMonoGetsTheForeignKey() throws Exception {
        givenApprovalTaskTable();
        givenUserTable();

        update("mono");

        assertThat(exectype("20260425120000-1")).isEqualTo("EXECUTED");
        assertThat(exectype("20260425120000-2"))
            .as("the foreign key must still be created where the user table exists")
            .isEqualTo("EXECUTED");
        assertThat(foreignKeyExists()).isTrue();
    }

    @Test
    void testConfigurationMigratesWithoutTheForeignKey() throws Exception {
        givenApprovalTaskTable();

        // No user table: this is the configuration context, and creating it here would defeat the test.
        assertThatCode(() -> update("configuration"))
            .as("the configuration context has no user table, so the migration must still complete")
            .doesNotThrowAnyException();

        assertThat(exectype("20260425120000-1"))
            .as("the columns and indexes are needed under configuration and must still apply")
            .isEqualTo("EXECUTED");
        assertThat(exectype("20260425120000-2"))
            .as("the foreign key changeset must be filtered out entirely, leaving no row behind")
            .isNull();
        assertThat(columnExists("approval_task", "assignee_id")).isTrue();
        assertThat(foreignKeyExists()).isFalse();
    }

    @Test
    void testTheForeignKeyIsNotAddedTwice() throws Exception {
        givenApprovalTaskTable();
        givenUserTable();

        update("mono");

        // An existing mono database that ran the unsplit changeset already carries the foreign key while having no
        // row for -2. Deleting the row reproduces that state without hand-writing databasechangelog columns.
        execute("DELETE FROM databasechangelog WHERE id = '20260425120000-2'");

        assertThatCode(() -> update("mono"))
            .as("re-running against a database that already has the foreign key must not abort the upgrade")
            .doesNotThrowAnyException();

        assertThat(exectype("20260425120000-2"))
            .as("the precondition must skip the change rather than add a duplicate constraint")
            .isEqualTo("MARK_RAN");
        assertThat(foreignKeyExists()).isTrue();
    }

    @Test
    void testAStaleChecksumOnTheSplitChangeSetStillValidates() throws Exception {
        givenApprovalTaskTable();
        givenUserTable();

        update("mono");

        // Every database that ran the unsplit -1 recorded a checksum for a body that no longer exists on disk. Any
        // foreign value reproduces that; the point is that validCheckSum accepts it instead of halting the upgrade.
        execute("UPDATE databasechangelog SET md5sum = '9:0123456789abcdef0123456789abcdef' "
            + "WHERE id = '20260425120000-1'");

        assertThatCode(() -> update("mono"))
            .as("validCheckSum must let a database that ran the unsplit changeset keep starting")
            .doesNotThrowAnyException();
    }

    private void givenApprovalTaskTable() throws SQLException {
        execute("CREATE TABLE approval_task (id BIGINT PRIMARY KEY, title VARCHAR(256))");
    }

    private void givenUserTable() throws SQLException {
        execute("CREATE TABLE \"user\" (id BIGINT PRIMARY KEY, login VARCHAR(256))");
    }

    private void update(String contexts) throws Exception {
        try (Connection connection = connection()) {
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(contexts), new LabelExpression());
            }
        }
    }

    @Nullable
    private String exectype(String changeSetId) throws SQLException {
        try (Connection connection = connection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT exectype FROM databasechangelog WHERE id = ?")) {

            preparedStatement.setString(1, changeSetId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        }
    }

    private boolean foreignKeyExists() throws SQLException {
        try (Connection connection = connection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_name = ? "
                    + "AND constraint_type = 'FOREIGN KEY'")) {

            preparedStatement.setString(1, FOREIGN_KEY);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        try (Connection connection = connection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = ? AND column_name = ?")) {

            preparedStatement.setString(1, tableName);
            preparedStatement.setString(2, columnName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    // Every string reaching this helper is a literal declared in this test class; there is no external input to
    // inject, and the DDL involved cannot be expressed as bind parameters.
    @SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    private void execute(String... sqls) throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String sql : sqls) {
                statement.execute(sql);
            }
        }
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
            postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
    }
}

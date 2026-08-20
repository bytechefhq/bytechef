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
 * Exercises the rollback path, which nothing else in the build does: every other changelog test only applies changes
 * forward, so the inverse Liquibase runs on a rollback has never been observed against a real database.
 *
 * <p>
 * The pairing here is the point. {@link #testAnExposedChangeSetDropsAColumnItNeverCreated()} runs the shape the
 * repository's changelogs used to have, a MARK_RAN precondition with no rollback block, and watches the auto-generated
 * {@code dropColumn} take out a column the changeset was skipped over and never created. The other two tests run the
 * real
 * {@code config/liquibase/changelog/automation/configuration/20260817000001_automation_configuration_project_visibility.xml}
 * through the same two paths and show its explicit empty {@code <rollback/>} leaving the column and its data alone.
 * Without the control test the other two would pass against a changeset that had simply never been rolled back.
 * </p>
 *
 * <p>
 * One changelog is enough. The hazard is a property of the {@code MARK_RAN} plus generated-inverse combination rather
 * than of any particular file, {@code LiquibaseMarkRanRollbackTest} statically holds every changelog to the same rule,
 * and standing up the schema each of the other sixty changesets needs would buy nothing this pair does not already
 * establish.
 * </p>
 *
 * @author Ivica Cardic
 */
@Testcontainers
class LiquibaseMarkRanRollbackIntTest {

    private static final String EXPOSED_CHANGELOG = "liquibase-rollback/exposed_add_column_changelog.xml";
    private static final String PROJECT_VISIBILITY_CHANGELOG =
        "config/liquibase/changelog/automation/configuration/20260817000001_automation_configuration_project_visibility.xml";

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeEach
    void beforeEach() throws SQLException {
        execute(
            "DROP TABLE IF EXISTS project", "DROP TABLE IF EXISTS databasechangelog",
            "DROP TABLE IF EXISTS databasechangeloglock");
    }

    @Test
    void testAnExposedChangeSetDropsAColumnItNeverCreated() throws Exception {
        givenProjectTableWithVisibilityColumn();

        assertThat(exectype(EXPOSED_CHANGELOG, "exposed-add-column"))
            .as("the column is already there, so the precondition must skip the change and mark the changeset ran")
            .isEqualTo("MARK_RAN");

        rollback(EXPOSED_CHANGELOG);

        // This is the data loss the empty rollback blocks exist to prevent. It is asserted rather than merely
        // described so the two tests below cannot pass for the wrong reason.
        assertThat(columnExists("project", "visibility"))
            .as("a changeset with no declared rollback hands Liquibase the inverse of its addColumn, and the drop runs"
                + " even though the precondition skipped the change")
            .isFalse();
    }

    @Test
    void testTheGuardedChangeSetLeavesAPreexistingColumnAloneOnRollback() throws Exception {
        givenProjectTableWithVisibilityColumn();

        assertThat(exectype(PROJECT_VISIBILITY_CHANGELOG, "20260817000001-01")).isEqualTo("MARK_RAN");

        rollback(PROJECT_VISIBILITY_CHANGELOG);

        assertThat(columnExists("project", "visibility"))
            .as("the empty rollback must leave a column this changeset never created in place")
            .isTrue();
        assertThat(visibilityOfProjectOne())
            .as("and the rows in it untouched")
            .isEqualTo(0);
    }

    @Test
    void testTheGuardedChangeSetLeavesTheColumnItAddedInPlaceOnRollback() throws Exception {
        givenProjectTableWithoutVisibilityColumn();

        assertThat(exectype(PROJECT_VISIBILITY_CHANGELOG, "20260817000001-01"))
            .as("the column is absent, so the changeset must actually apply")
            .isEqualTo("EXECUTED");
        assertThat(visibilityOfProjectOne())
            .as("existing rows default to WORKSPACE")
            .isEqualTo(1);

        rollback(PROJECT_VISIBILITY_CHANGELOG);

        // The deliberate cost of the empty rollback: an inert leftover column, chosen over a drop that cannot be
        // distinguished from the destructive case above.
        assertThat(columnExists("project", "visibility"))
            .as("the empty rollback keeps the column even where the changeset did create it; that is the accepted cost")
            .isTrue();
    }

    private void givenProjectTableWithVisibilityColumn() throws SQLException {
        execute(
            "CREATE TABLE project (id BIGINT PRIMARY KEY, name VARCHAR(256), visibility INT NOT NULL DEFAULT 1)",
            "INSERT INTO project (id, name, visibility) VALUES (1, 'preexisting', 0)");
    }

    private void givenProjectTableWithoutVisibilityColumn() throws SQLException {
        execute(
            "CREATE TABLE project (id BIGINT PRIMARY KEY, name VARCHAR(256))",
            "INSERT INTO project (id, name) VALUES (1, 'preexisting')");
    }

    private String exectype(String changeLogFile, String changeSetId) throws Exception {
        withLiquibase(changeLogFile, liquibase -> liquibase.update(new Contexts(), new LabelExpression()));

        try (Connection connection = connection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT exectype FROM databasechangelog WHERE id = ?")) {

            preparedStatement.setString(1, changeSetId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                boolean found = resultSet.next();

                assertThat(found)
                    .as("changeset %s left no row in databasechangelog", changeSetId)
                    .isTrue();

                return resultSet.getString(1);
            }
        }
    }

    private void rollback(String changeLogFile) throws Exception {
        withLiquibase(changeLogFile, liquibase -> liquibase.rollback(1, new Contexts(), new LabelExpression()));
    }

    private void withLiquibase(String changeLogFile, LiquibaseCallback callback) throws Exception {
        try (Connection connection = connection()) {
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            try (Liquibase liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database)) {
                callback.accept(liquibase);
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

    @Nullable
    private Integer visibilityOfProjectOne() throws SQLException {
        try (Connection connection = connection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT visibility FROM project WHERE id = 1");
            ResultSet resultSet = preparedStatement.executeQuery()) {

            return resultSet.next() ? resultSet.getInt(1) : null;
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

    @FunctionalInterface
    private interface LiquibaseCallback {

        void accept(Liquibase liquibase) throws Exception;
    }
}

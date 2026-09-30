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

package com.bytechef.platform.data.table.configuration.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@SuppressFBWarnings({
    "SQL_INJECTION_SPRING_JDBC", "SQL_INJECTION_JDBC", "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"
})
class DataTableWorkspaceScopedNamingMigratorIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DataTableWorkspaceScopedNamingMigrator dataTableWorkspaceScopedNamingMigrator;

    @BeforeEach
    void beforeEach() {
        dataTableWorkspaceScopedNamingMigrator = new DataTableWorkspaceScopedNamingMigrator(jdbcTemplate);

        MigratorFixtures.createLinkTable(jdbcTemplate);
    }

    @AfterEach
    void afterEach() {
        MigratorFixtures.dropAll(jdbcTemplate);
    }

    @Test
    void testRenamesALegacyPhysicalTableToItsId() {
        long ordersId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyorders");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, ordersId, 1L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacyorders", "first");

        dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(MigratorFixtures.tableExists(jdbcTemplate, "dt_0_legacyorders")).isFalse();
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + ordersId)).containsExactly("first");
        assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, ordersId)).isEqualTo(1L);
    }

    @Test
    void testRenamesLegacyPhysicalTablesInEveryEnvironment() {
        long ordersId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacymultienv");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, ordersId, 1L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacymultienv", "dev-row");
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_2_legacymultienv", "prod-row");

        DataTableWorkspaceScopedNamingMigrator.MigrationResult migrationResult =
            dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(migrationResult.renamedTableCount()).isEqualTo(2);
        assertThat(MigratorFixtures.tableExists(jdbcTemplate, "dt_0_legacymultienv")).isFalse();
        assertThat(MigratorFixtures.tableExists(jdbcTemplate, "dt_2_legacymultienv")).isFalse();
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + ordersId)).containsExactly("dev-row");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_2_" + ordersId)).containsExactly("prod-row");
    }

    @Test
    void testSplitsATableLinkedToTwoWorkspaces() {
        long sharedId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyshared");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 1L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 2L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacyshared", "row");
        MigratorFixtures.addTag(jdbcTemplate, sharedId, 7L);
        MigratorFixtures.addWebhook(jdbcTemplate, sharedId, "https://example.com/hook");

        Logger logger = (Logger) LoggerFactory.getLogger(DataTableWorkspaceScopedNamingMigrator.class);

        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        logAppender.start();
        logger.addAppender(logAppender);

        DataTableWorkspaceScopedNamingMigrator.MigrationResult migrationResult;

        try {
            migrationResult = dataTableWorkspaceScopedNamingMigrator.migrate(null);
        } finally {
            logger.detachAppender(logAppender);
        }

        assertThat(migrationResult.splitTableCount()).isEqualTo(1);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, workspace_id FROM data_table WHERE name = 'legacyshared' ORDER BY workspace_id");

        assertThat(rows).hasSize(2);

        long copyId = ((Number) rows.get(1)
            .get("id")).longValue();

        assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, sharedId)).isEqualTo(1L);
        assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, copyId)).isEqualTo(2L);
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + sharedId)).containsExactly("row");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + copyId)).containsExactly("row");
        assertThat(MigratorFixtures.tagIds(jdbcTemplate, copyId)).containsExactly(7L);
        assertThat(MigratorFixtures.webhookUrls(jdbcTemplate, sharedId)).isEmpty();
        assertThat(MigratorFixtures.webhookUrls(jdbcTemplate, copyId)).isEmpty();
        assertThat(logAppender.list)
            .anyMatch(
                event -> event.getLevel() == Level.WARN && event.getFormattedMessage()
                    .equals(
                        "Removed webhook https://example.com/hook from shared data table 'legacyshared' (id="
                            + sharedId + ") while splitting it by workspace; re-enable the triggers that used it"));

        jdbcTemplate.update("INSERT INTO \"dt_0_" + copyId + "\" (\"title\") VALUES ('next')");

        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + copyId)).containsExactlyInAnyOrder("row", "next");
    }

    @Test
    void testSplitsATableLinkedToThreeWorkspaces() {
        long sharedId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacytriple");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 1L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 2L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 3L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacytriple", "row");
        MigratorFixtures.addWebhook(jdbcTemplate, sharedId, "https://example.com/triple");

        DataTableWorkspaceScopedNamingMigrator.MigrationResult migrationResult =
            dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(migrationResult.splitTableCount()).isEqualTo(1);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, workspace_id FROM data_table WHERE name = 'legacytriple' ORDER BY workspace_id");

        assertThat(rows).hasSize(3);

        long secondCopyId = ((Number) rows.get(1)
            .get("id")).longValue();
        long thirdCopyId = ((Number) rows.get(2)
            .get("id")).longValue();

        assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, sharedId)).isEqualTo(1L);
        assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, secondCopyId)).isEqualTo(2L);
        assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, thirdCopyId)).isEqualTo(3L);
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + sharedId)).containsExactly("row");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + secondCopyId)).containsExactly("row");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + thirdCopyId)).containsExactly("row");
        assertThat(MigratorFixtures.webhookUrls(jdbcTemplate, sharedId)).isEmpty();
        assertThat(MigratorFixtures.webhookUrls(jdbcTemplate, secondCopyId)).isEmpty();
        assertThat(MigratorFixtures.webhookUrls(jdbcTemplate, thirdCopyId)).isEmpty();
    }

    @Test
    void testLeavesAnUnregisteredPhysicalTableAlone() {
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_orphanleftover", "orphan");

        dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(MigratorFixtures.tableExists(jdbcTemplate, "dt_0_orphanleftover")).isTrue();
    }

    @Test
    void testWarnsAboutAPhysicalTableThatMatchesNoRegisteredDataTable() {
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_orphanwarn", "orphan");

        Logger logger = (Logger) LoggerFactory.getLogger(DataTableWorkspaceScopedNamingMigrator.class);

        ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        logAppender.start();
        logger.addAppender(logAppender);

        try {
            dataTableWorkspaceScopedNamingMigrator.migrate(null);
        } finally {
            logger.detachAppender(logAppender);
        }

        assertThat(logAppender.list)
            .anyMatch(
                event -> event.getLevel() == Level.WARN && event.getFormattedMessage()
                    .contains("dt_0_orphanwarn"));
    }

    @Test
    void testDropsTheLinkTable() {
        dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(MigratorFixtures.tableExists(jdbcTemplate, "workspace_data_table")).isFalse();
    }

    @Test
    void testRunsWithoutALinkTable() {
        MigratorFixtures.dropLinkTable(jdbcTemplate);

        long ordersId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacynolink");

        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacynolink", "kept");

        dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + ordersId)).containsExactly("kept");
    }

    @Test
    void testRunningMigrateTwiceProducesTheSameEndState() {
        long sharedId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyrerun");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 1L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 2L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacyrerun", "row");

        dataTableWorkspaceScopedNamingMigrator.migrate(null);

        List<Map<String, Object>> rowsAfterFirstRun = jdbcTemplate.queryForList(
            "SELECT id, workspace_id FROM data_table WHERE name = 'legacyrerun' ORDER BY workspace_id");

        DataTableWorkspaceScopedNamingMigrator.MigrationResult secondRunResult =
            dataTableWorkspaceScopedNamingMigrator.migrate(null);

        List<Map<String, Object>> rowsAfterSecondRun = jdbcTemplate.queryForList(
            "SELECT id, workspace_id FROM data_table WHERE name = 'legacyrerun' ORDER BY workspace_id");

        assertThat(secondRunResult.splitTableCount()).isZero();
        assertThat(secondRunResult.renamedTableCount()).isZero();
        assertThat(rowsAfterSecondRun).isEqualTo(rowsAfterFirstRun);

        long copyId = ((Number) rowsAfterFirstRun.get(1)
            .get("id")).longValue();

        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + sharedId)).containsExactly("row");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + copyId)).containsExactly("row");
    }

    /**
     * H2 auto-commits DDL statement by statement, so a crash between the rename step and dropping the link table leaves
     * exactly this state: the split already completed (registry rows, tags and physical tables all copied) and the
     * original's physical table already renamed to its id, but the link table still present. A resumed run must
     * recognize all of that as already done rather than duplicate it or collide with it.
     */
    @Test
    void testRecoversFromAPartialRunThatLeftTheLinkTablePresent() {
        long originalId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacypartial");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, originalId, 1L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, originalId, 2L);
        MigratorFixtures.setWorkspaceId(jdbcTemplate, originalId, 1L);

        long copyId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacypartial");

        MigratorFixtures.setWorkspaceId(jdbcTemplate, copyId, 2L);
        MigratorFixtures.addTag(jdbcTemplate, copyId, 9L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_" + originalId, "kept-original");
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_" + copyId, "kept-copy");

        DataTableWorkspaceScopedNamingMigrator.MigrationResult migrationResult =
            dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(migrationResult.splitTableCount()).isEqualTo(1);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM data_table WHERE name = 'legacypartial' ORDER BY workspace_id");

        assertThat(rows).hasSize(2);
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + originalId)).containsExactly("kept-original");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + copyId)).containsExactly("kept-copy");
        assertThat(MigratorFixtures.tagIds(jdbcTemplate, copyId)).containsExactly(9L);
        assertThat(MigratorFixtures.webhookUrls(jdbcTemplate, originalId)).isEmpty();
        assertThat(MigratorFixtures.tableExists(jdbcTemplate, "workspace_data_table")).isFalse();
    }

    @Test
    void testFailsWithADescriptiveMessageForAnUnsupportedColumnType() {
        long sharedId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyunsupported");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 1L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 2L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacyunsupported", "row");
        jdbcTemplate.execute("ALTER TABLE \"dt_0_legacyunsupported\" ADD COLUMN \"payload\" BYTEA");

        assertThatThrownBy(() -> dataTableWorkspaceScopedNamingMigrator.migrate(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("dt_0_legacyunsupported")
            .hasMessageContaining("payload")
            .hasMessageContaining("bytea");
    }

    /**
     * Reproduces the exact constraint shape the migration runs against on a real upgrade: the old, single-column
     * {@code UNIQUE(name)} is still in effect until changeset -2 drops it, and the new composite constraint is not
     * added until changeset -4, after the split has already run. A split's copy intentionally reuses the original's
     * {@code name}, so the ordering is load-bearing, not cosmetic -- this proves it rather than assuming the changelog
     * got it right.
     */
    @Test
    void testTheOldGlobalNameConstraintMustBeDroppedBeforeSplittingAndTheNewOneAddedAfter() {
        jdbcTemplate.execute("ALTER TABLE data_table DROP CONSTRAINT uk_data_table_workspace_name");
        jdbcTemplate.execute("ALTER TABLE data_table ADD CONSTRAINT uk_data_table_name UNIQUE (name)");

        try {
            long sharedId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyconstraintorder");

            MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 1L);
            MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 2L);
            MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacyconstraintorder", "row");

            assertThatThrownBy(() -> dataTableWorkspaceScopedNamingMigrator.migrate(null))
                .isInstanceOf(DataIntegrityViolationException.class);

            jdbcTemplate.execute("ALTER TABLE data_table DROP CONSTRAINT uk_data_table_name");

            DataTableWorkspaceScopedNamingMigrator.MigrationResult migrationResult =
                dataTableWorkspaceScopedNamingMigrator.migrate(null);

            assertThat(migrationResult.splitTableCount()).isEqualTo(1);

            assertThatCode(
                () -> jdbcTemplate.execute(
                    "ALTER TABLE data_table ADD CONSTRAINT uk_data_table_workspace_name UNIQUE (workspace_id, name)"))
                        .doesNotThrowAnyException();
        } finally {
            jdbcTemplate.execute("ALTER TABLE data_table DROP CONSTRAINT IF EXISTS uk_data_table_name");
            jdbcTemplate.execute("ALTER TABLE data_table DROP CONSTRAINT IF EXISTS uk_data_table_workspace_name");
            jdbcTemplate.execute(
                "ALTER TABLE data_table ADD CONSTRAINT uk_data_table_workspace_name UNIQUE (workspace_id, name)");
        }
    }

    /**
     * Proves the {@code sqlCheck} precondition guarding changeset -2 is scoped to the schema being migrated, not to the
     * whole database. {@code MultiTenantSpringLiquibase} migrates one tenant schema at a time, so an unscoped
     * {@code COUNT(*)} across {@code information_schema.table_constraints} would see every other tenant's copy of the
     * same constraint name -- a schema that has already dropped it could still read a count of one or more from a
     * schema that has not, or a schema that still has it could read a count from an unrelated schema and be told to
     * skip the drop. The schema compared is Liquibase's default schema, substituted into
     * {@code ${database.defaultSchemaName}} the way Liquibase expands changelog properties, not the connection's
     * {@code search_path}, which a per-tenant run does not necessarily move; only an empty default schema falls back to
     * {@code current_schema()}. Runs the exact SQL text from the changelog, read off the classpath resource rather than
     * duplicated by hand, so a future edit to the precondition cannot silently drift out of sync with this test.
     */
    @Test
    void testTheUniqueConstraintGuardIsScopedToTheMigratedSchema() {
        String otherSchema = "dt_migration_constraint_guard";
        String preconditionSql = readUniqueConstraintDroppedPreconditionSql();

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + otherSchema + "\" CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA \"" + otherSchema + "\"");

        try {
            jdbcTemplate.execute(
                "CREATE TABLE \"" + otherSchema + "\".data_table (id BIGSERIAL PRIMARY KEY, "
                    + "name VARCHAR(256) NOT NULL)");
            jdbcTemplate.execute(
                "ALTER TABLE \"" + otherSchema + "\".data_table ADD CONSTRAINT uk_data_table_name UNIQUE (name)");

            Integer countInPublicSchema = jdbcTemplate.queryForObject(
                substituteDefaultSchemaName(preconditionSql, "public"), Integer.class);

            assertThat(countInPublicSchema).isZero();

            Integer countInOtherSchema = jdbcTemplate.queryForObject(
                substituteDefaultSchemaName(preconditionSql, otherSchema), Integer.class);

            assertThat(countInOtherSchema).isEqualTo(1);

            Integer countInOtherSchemaIgnoringSearchPath = runWithSearchPath(
                otherSchema, substituteDefaultSchemaName(preconditionSql, "public"));

            assertThat(countInOtherSchemaIgnoringSearchPath).isZero();

            Integer countFallingBackToSearchPath = runWithSearchPath(
                otherSchema, substituteDefaultSchemaName(preconditionSql, ""));

            assertThat(countFallingBackToSearchPath).isEqualTo(1);
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + otherSchema + "\" CASCADE");
        }
    }

    /**
     * Runs {@code sql} with the connection's {@code search_path} pointed at {@code schema}, and restores it on the same
     * physical connection before returning it to the pool -- a plain {@code SET search_path} followed by a separate
     * reset call could land on two different pooled connections and leak the change onto whichever connection a later
     * test happens to be handed.
     */
    private Integer runWithSearchPath(String schema, String sql) {
        return jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            try (Statement setStatement = connection.createStatement()) {
                setStatement.execute("SET search_path TO \"" + schema + "\"");

                try (Statement queryStatement = connection.createStatement();
                    ResultSet resultSet = queryStatement.executeQuery(sql)) {

                    resultSet.next();

                    return resultSet.getInt(1);
                }
            } finally {
                try (Statement resetStatement = connection.createStatement()) {
                    resetStatement.execute("SET search_path TO public");
                }
            }
        });
    }

    private static String substituteDefaultSchemaName(String sql, String defaultSchemaName) {
        if (!sql.contains("${database.defaultSchemaName}")) {
            throw new IllegalStateException("sqlCheck precondition does not reference ${database.defaultSchemaName}");
        }

        return sql.replace("${database.defaultSchemaName}", defaultSchemaName);
    }

    private static String readUniqueConstraintDroppedPreconditionSql() {
        String changelogXml = readClasspathResource(
            "config/liquibase/changelog/platform/data_table/"
                + "20260916000001_platform_data_table_workspace_scoped_naming.xml");

        Matcher matcher = Pattern.compile("<sqlCheck expectedResult=\"1\">(.*?)</sqlCheck>", Pattern.DOTALL)
            .matcher(changelogXml);

        if (!matcher.find()) {
            throw new IllegalStateException("sqlCheck precondition not found in changelog");
        }

        return matcher.group(1)
            .trim();
    }

    private static String readClasspathResource(String path) {
        try (InputStream inputStream = DataTableWorkspaceScopedNamingMigratorIntTest.class.getClassLoader()
            .getResourceAsStream(path)) {

            Objects.requireNonNull(inputStream, "resource not found: " + path);

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
    }

    /**
     * Liquibase's per-tenant run sets its own default schema without necessarily moving the JDBC connection's
     * {@code search_path}, so a schema argument that differs from the connection's default has to be honored on every
     * statement -- a table or row left over in the connection's own default schema proves nothing was read from or
     * written to it by mistake.
     */
    @Test
    void testMigratesOnlyTheGivenSchemaEvenWhenItDiffersFromTheConnectionsDefaultSchema() {
        String otherSchema = "dt_migration_other";

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + otherSchema + "\" CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA \"" + otherSchema + "\"");

        try {
            jdbcTemplate.execute(
                "CREATE TABLE \"" + otherSchema + "\".data_table (id BIGSERIAL PRIMARY KEY, "
                    + "name VARCHAR(256) NOT NULL, description TEXT, workspace_id BIGINT, "
                    + "created_date TIMESTAMP NOT NULL, created_by VARCHAR(50) NOT NULL, "
                    + "last_modified_date TIMESTAMP NOT NULL, last_modified_by VARCHAR(50) NOT NULL, "
                    + "version BIGINT NOT NULL)");
            jdbcTemplate.execute(
                "CREATE TABLE \"" + otherSchema + "\".data_table_tag (data_table_id BIGINT NOT NULL, "
                    + "tag_id BIGINT NOT NULL)");
            jdbcTemplate.execute(
                "CREATE TABLE \"" + otherSchema + "\".data_table_webhook (id BIGSERIAL PRIMARY KEY, "
                    + "data_table_id BIGINT NOT NULL, url VARCHAR(2000) NOT NULL, type INT NOT NULL, "
                    + "environment INT NOT NULL, created_date TIMESTAMP NOT NULL, created_by VARCHAR(50) NOT NULL, "
                    + "last_modified_date TIMESTAMP NOT NULL, last_modified_by VARCHAR(50) NOT NULL, "
                    + "version BIGINT NOT NULL)");
            jdbcTemplate.execute(
                "CREATE TABLE \"" + otherSchema + "\".workspace_data_table (id BIGSERIAL PRIMARY KEY, "
                    + "data_table_id BIGINT NOT NULL, workspace_id BIGINT NOT NULL, "
                    + "created_date TIMESTAMP NOT NULL, created_by VARCHAR(50) NOT NULL, "
                    + "last_modified_date TIMESTAMP NOT NULL, last_modified_by VARCHAR(50) NOT NULL, "
                    + "version BIGINT NOT NULL)");

            long otherSchemaOrdersId = insertDataTableInSchema(otherSchema, "legacyother");

            jdbcTemplate.update(
                "INSERT INTO \"" + otherSchema + "\".workspace_data_table (data_table_id, workspace_id, "
                    + "created_date, created_by, last_modified_date, last_modified_by, version) "
                    + "VALUES (?, 9, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
                otherSchemaOrdersId);

            jdbcTemplate.execute(
                "CREATE TABLE \"" + otherSchema + "\".\"dt_0_legacyother\" (\"id\" BIGSERIAL PRIMARY KEY, "
                    + "\"external_id\" VARCHAR(255), \"title\" VARCHAR(255))");
            jdbcTemplate.update(
                "INSERT INTO \"" + otherSchema + "\".\"dt_0_legacyother\" (\"title\") VALUES ('other-row')");

            long publicOrdersId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacypublicguard");

            MigratorFixtures.linkToWorkspace(jdbcTemplate, publicOrdersId, 5L);
            MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacypublicguard", "public-row");

            dataTableWorkspaceScopedNamingMigrator.migrate(otherSchema);

            Integer otherSchemaLegacyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                Integer.class, otherSchema, "dt_0_legacyother");

            assertThat(otherSchemaLegacyCount).isZero();

            List<String> otherSchemaTitles = jdbcTemplate.queryForList(
                "SELECT \"title\" FROM \"" + otherSchema + "\".\"dt_0_" + otherSchemaOrdersId + "\"", String.class);

            assertThat(otherSchemaTitles).containsExactly("other-row");

            Long otherSchemaWorkspaceId = jdbcTemplate.queryForObject(
                "SELECT workspace_id FROM \"" + otherSchema + "\".data_table WHERE id = ?", Long.class,
                otherSchemaOrdersId);

            assertThat(otherSchemaWorkspaceId).isEqualTo(9L);

            assertThat(MigratorFixtures.tableExists(jdbcTemplate, "dt_0_legacypublicguard")).isTrue();
            assertThat(MigratorFixtures.tableExists(jdbcTemplate, "dt_0_" + publicOrdersId)).isFalse();
            assertThat(MigratorFixtures.workspaceIdOf(jdbcTemplate, publicOrdersId)).isNull();
        } finally {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + otherSchema + "\" CASCADE");
        }
    }

    private long insertDataTableInSchema(String schema, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO \"" + schema + "\".data_table (name, created_date, created_by, last_modified_date, "
                    + "last_modified_by, version) VALUES (?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, "
                    + "'test', 0)",
                new String[] {
                    "id"
            });

            preparedStatement.setString(1, name);

            return preparedStatement;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey())
            .longValue();
    }
}

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
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class DataTableWorkspaceScopedNamingMigratorH2IntTest {

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
     * H2 auto-commits DDL and DML as separate statements, so a run that crashes between creating a copy's physical
     * table and running its {@code INSERT ... SELECT} leaves the copy present, correctly shaped, and permanently empty.
     * A resumed run must notice the copy has no rows while its source does, and repair it rather than treating "the
     * table exists" as "the table is done".
     */
    @Test
    void testRepopulatesACopyPhysicalTableThatWasCreatedButNeverFilled() {
        long sharedId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyemptycopy");

        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 1L);
        MigratorFixtures.linkToWorkspace(jdbcTemplate, sharedId, 2L);
        MigratorFixtures.createLegacyTable(jdbcTemplate, "dt_0_legacyemptycopy", "row");

        long copyId = MigratorFixtures.insertDataTable(jdbcTemplate, "legacyemptycopy");

        MigratorFixtures.setWorkspaceId(jdbcTemplate, copyId, 2L);

        jdbcTemplate.execute(
            "CREATE TABLE \"dt_0_" + copyId + "\" (\"id\" BIGSERIAL PRIMARY KEY, \"external_id\" VARCHAR(255), "
                + "\"title\" VARCHAR(255))");
        jdbcTemplate.execute("CREATE UNIQUE INDEX ON \"dt_0_" + copyId + "\" (\"external_id\")");

        dataTableWorkspaceScopedNamingMigrator.migrate(null);

        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + sharedId)).containsExactly("row");
        assertThat(MigratorFixtures.titles(jdbcTemplate, "dt_0_" + copyId)).containsExactly("row");
    }
}

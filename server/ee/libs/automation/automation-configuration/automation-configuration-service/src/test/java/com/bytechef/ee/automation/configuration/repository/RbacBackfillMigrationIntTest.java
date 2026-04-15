/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Multi-phase scenario test for the RBAC backfill and unique-constraint migrations. The smoke test
 * {@code RbacMigrationsIntTest} only verifies migrations apply cleanly to a fresh database; this test exercises the
 * load-bearing scenarios on populated databases:
 *
 * <ul>
 * <li><b>Backfill on populated DB:</b> existing EDITOR rows promoted to ADMIN; existing ADMIN rows untouched;
 * {@code project_user} rows seeded for every (workspace_user × project-in-that-workspace) pair.</li>
 * <li><b>Backfill idempotency:</b> re-running the changeset (after {@code clearCheckSums} forces re-application) does
 * not create duplicate {@code project_user} rows thanks to {@code WHERE NOT EXISTS}.</li>
 * <li><b>Unique-constraint dedup:</b> a database with pre-existing duplicate {@code (workspace_id, user_id)} rows gets
 * deduped (oldest id wins) before the constraint is added — without this dedup, the migration would fail startup on EE
 * deployments that pre-date the constraint.</li>
 * </ul>
 *
 * <p>
 * <b>Strategy:</b> Spring Boot runs all migrations on context startup (the backfill is a no-op via {@code
 * <preConditions>} since {@code workspace_user} starts empty). Each test then "un-runs" the relevant changeset by
 * deleting its {@code databasechangelog} row, injects pre-existing data, and re-triggers the changelog file via the
 * Liquibase Java API. This avoids disabling Spring Boot's autorun while still exercising the migration end-to-end on a
 * populated database.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class RbacBackfillMigrationIntTest {

    private static final String BACKFILL_CHANGESET_ID = "202604061200050-backfill-project-user";
    private static final String UNIQUE_CONSTRAINT_CHANGESET_ID = "202604061200060-dedup-and-unique-workspace-user";
    private static final String BACKFILL_CHANGELOG =
        "config/liquibase/changelog/automation/configuration/"
            + "202604061200050_automation_configuration_backfill_project_user.xml";
    private static final String UNIQUE_CONSTRAINT_CHANGELOG =
        "config/liquibase/changelog/automation/configuration/"
            + "202604061200060_automation_configuration_workspace_user_unique_constraint.xml";

    private static final int ADMIN_ORDINAL = 0;
    private static final int EDITOR_ORDINAL = 1;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);

        // Each test starts from a clean RBAC slate. Other tables (created by other Liquibase migrations) are left
        // alone — they are not under test here. rbac_backfill_report is cleared too so assertions on "this run
        // added exactly one audit row" are not polluted by prior test executions (the backfill is re-run per test
        // via rerunChangelog, and each re-run appends a new audit row by design).
        jdbc.update("DELETE FROM project_user");
        jdbc.update("DELETE FROM workspace_user");
        jdbc.update("DELETE FROM project");
        jdbc.update("DELETE FROM workspace");
        jdbc.update("DELETE FROM rbac_backfill_report");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM project_user");
        jdbc.update("DELETE FROM workspace_user");
        jdbc.update("DELETE FROM project");
        jdbc.update("DELETE FROM workspace");
        jdbc.update("DELETE FROM rbac_backfill_report");
    }

    @Test
    void testBackfillPromotesEditorsAndSeedsProjectAdmins() throws Exception {
        // Pre-existing state: a workspace with two members (one EDITOR, one already ADMIN) and two projects. After
        // the backfill, both should be ADMIN at workspace level and ADMIN on every project in the workspace.
        long workspaceId = insertWorkspace("ws-1");
        long otherWorkspaceId = insertWorkspace("ws-other");
        long projectA = insertProject("project-A", workspaceId);
        long projectB = insertProject("project-B", workspaceId);
        long projectInOtherWorkspace = insertProject("project-other", otherWorkspaceId);

        long editorUserId = 100L;
        long adminUserId = 200L;
        long otherWorkspaceUserId = 300L;

        insertWorkspaceUser(workspaceId, editorUserId, EDITOR_ORDINAL);
        insertWorkspaceUser(workspaceId, adminUserId, ADMIN_ORDINAL);
        insertWorkspaceUser(otherWorkspaceId, otherWorkspaceUserId, EDITOR_ORDINAL);

        rerunChangelog(BACKFILL_CHANGELOG, BACKFILL_CHANGESET_ID);

        // Workspace roles: EDITOR (1) → ADMIN (0); existing ADMIN unchanged.
        assertThat(workspaceRoleOf(workspaceId, editorUserId)).isEqualTo(ADMIN_ORDINAL);
        assertThat(workspaceRoleOf(workspaceId, adminUserId)).isEqualTo(ADMIN_ORDINAL);
        assertThat(workspaceRoleOf(otherWorkspaceId, otherWorkspaceUserId)).isEqualTo(ADMIN_ORDINAL);

        // Project memberships: ADMIN (ordinal 0) seeded for every workspace member × project in that workspace.
        // 2 users × 2 projects in workspace 1 = 4 rows; 1 user × 1 project in workspace other = 1 row. Total 5.
        assertThat(projectUserCount()).isEqualTo(5);

        assertThat(projectRoleOf(projectA, editorUserId)).isEqualTo(ADMIN_ORDINAL);
        assertThat(projectRoleOf(projectA, adminUserId)).isEqualTo(ADMIN_ORDINAL);
        assertThat(projectRoleOf(projectB, editorUserId)).isEqualTo(ADMIN_ORDINAL);
        assertThat(projectRoleOf(projectB, adminUserId)).isEqualTo(ADMIN_ORDINAL);
        assertThat(projectRoleOf(projectInOtherWorkspace, otherWorkspaceUserId)).isEqualTo(ADMIN_ORDINAL);

        // Audit row captures the blast radius as deltas (not totals): 2 EDITOR rows promoted to ADMIN this run
        // (1 in ws-1 + 1 in ws-other; the pre-existing ADMIN in ws-1 is not touched by WHERE workspace_role = 1)
        // and 5 project_user rows seeded (2×2 in ws-1 + 1×1 in ws-other). The pre-existing ADMIN still gets project
        // rows seeded because they had no project_user rows before \u2014 the seed key is (project, user), not
        // (project, user, role). Counting deltas instead of post-state totals is what makes a clearCheckSums re-run
        // append a 0/0 audit row instead of double-counting prior runs' work.
        List<Map<String, Object>> auditRows = jdbc.queryForList(
            "SELECT workspace_users_promoted, project_users_seeded FROM rbac_backfill_report "
                + "ORDER BY executed_at DESC");

        assertThat(auditRows).hasSize(1);
        assertThat(((Number) auditRows.get(0)
            .get("workspace_users_promoted")).longValue()).isEqualTo(2L);
        assertThat(((Number) auditRows.get(0)
            .get("project_users_seeded")).longValue()).isEqualTo(5L);
    }

    @Test
    void testBackfillStampsRowsWithRbacBackfillSentinel() throws Exception {
        // Every project_user row inserted by the backfill is tagged created_by='rbac-backfill' so operators can
        // distinguish backfill-inserted admin grants from application-inserted 'system' rows during forensic audits
        // or dev-mode rollbacks. Without the sentinel, the two populations are indistinguishable and any DELETE
        // intended to drop backfill rows could silently sweep legitimate application data.
        long workspaceId = insertWorkspace("ws-sentinel");
        long projectId = insertProject("project-sentinel", workspaceId);
        long userId = 777L;

        insertWorkspaceUser(workspaceId, userId, EDITOR_ORDINAL);

        rerunChangelog(BACKFILL_CHANGELOG, BACKFILL_CHANGESET_ID);

        List<Map<String, Object>> backfillRows = jdbc.queryForList(
            "SELECT created_by, last_modified_by FROM project_user WHERE project_id = ? AND user_id = ?",
            projectId, userId);

        assertThat(backfillRows).hasSize(1);
        assertThat(backfillRows.get(0)
            .get("created_by")).isEqualTo("rbac-backfill");
        assertThat(backfillRows.get(0)
            .get("last_modified_by")).isEqualTo("rbac-backfill");
    }

    @Test
    void testBackfillIsIdempotentOnRerun() throws Exception {
        long workspaceId = insertWorkspace("ws-1");
        long projectId = insertProject("project-A", workspaceId);

        long userId = 100L;

        insertWorkspaceUser(workspaceId, userId, EDITOR_ORDINAL);

        // First application: promote + seed.
        rerunChangelog(BACKFILL_CHANGELOG, BACKFILL_CHANGESET_ID);

        long projectUserCountAfterFirst = projectUserCount();
        Integer projectRoleAfterFirst = projectRoleOf(projectId, userId);

        // Simulate a clearCheckSums + restart by un-running the changeset row again. The data in workspace_user and
        // project_user is unchanged from the first run.
        rerunChangelog(BACKFILL_CHANGELOG, BACKFILL_CHANGESET_ID);

        // Project_user count must not double — WHERE NOT EXISTS prevents duplicate seeding.
        assertThat(projectUserCount()).isEqualTo(projectUserCountAfterFirst);
        // Project_user role unchanged (still ADMIN).
        assertThat(projectRoleOf(projectId, userId)).isEqualTo(projectRoleAfterFirst);

        // Audit rows record per-run deltas, not cumulative totals. The first run promoted/seeded 1 row each; the
        // re-run touched nothing and must record 0/0. A naive SELECT-from-post-state count (the original migration)
        // would have recorded 1/1 on the second run too, double-counting prior work and misleading operators
        // auditing the blast radius.
        List<Map<String, Object>> auditRows = jdbc.queryForList(
            "SELECT workspace_users_promoted, project_users_seeded FROM rbac_backfill_report "
                + "ORDER BY executed_at ASC, id ASC");

        assertThat(auditRows).hasSize(2);
        assertThat(((Number) auditRows.get(0)
            .get("workspace_users_promoted")).longValue()).isEqualTo(1L);
        assertThat(((Number) auditRows.get(0)
            .get("project_users_seeded")).longValue()).isEqualTo(1L);
        assertThat(((Number) auditRows.get(1)
            .get("workspace_users_promoted")).longValue()).isEqualTo(0L);
        assertThat(((Number) auditRows.get(1)
            .get("project_users_seeded")).longValue()).isEqualTo(0L);
    }

    @Test
    void testBackfillSkipsRerunGuardOnDemotedAdmin() throws Exception {
        // Documented edge case: the WHERE workspace_role = 1 filter only re-promotes EDITOR rows. If an admin was
        // demoted to EDITOR (workspace_role = 1) between original backfill and clearCheckSums-forced rerun, that
        // demoted admin WILL be re-promoted to ADMIN. The migration comment calls this out as a known limitation.
        // This test pins the behavior so a future change does not silently alter it.
        long workspaceId = insertWorkspace("ws-1");
        long demotedAdminUserId = 100L;

        insertWorkspaceUser(workspaceId, demotedAdminUserId, EDITOR_ORDINAL);

        rerunChangelog(BACKFILL_CHANGELOG, BACKFILL_CHANGESET_ID);

        assertThat(workspaceRoleOf(workspaceId, demotedAdminUserId)).isEqualTo(ADMIN_ORDINAL);
    }

    @Test
    void testUniqueConstraintMigrationDedupsPreservingMostPrivilegedRole() throws Exception {
        // Inject duplicates for the same (workspace_id, user_id) pair with mixed roles. The migration must keep the
        // ADMIN row (most privileged) and delete the EDITOR rows \u2014 a MIN(id) strategy would silently downgrade
        // this user from ADMIN to EDITOR with no audit trail. Also covers the tiebreaker: when two rows share the
        // most-privileged role, the older one (MIN(id)) wins.
        long workspaceId = insertWorkspace("ws-1");
        long userId = 100L;
        long otherUserId = 200L;

        // Drop the existing constraint so we can re-run the migration cleanly. (After Spring Boot, the constraint
        // is in place and would reject the duplicate inserts below.)
        jdbc.update("ALTER TABLE workspace_user DROP CONSTRAINT IF EXISTS uk_workspace_user_workspace_user");

        // Duplicates for user 100: EDITOR (older) then ADMIN then EDITOR. The ADMIN row must survive.
        long firstId = insertWorkspaceUser(workspaceId, userId, EDITOR_ORDINAL);
        long secondId = insertWorkspaceUser(workspaceId, userId, ADMIN_ORDINAL);
        long thirdId = insertWorkspaceUser(workspaceId, userId, EDITOR_ORDINAL);

        assertThat(firstId).isLessThan(secondId);
        assertThat(secondId).isLessThan(thirdId);

        // Duplicates for user 200: two ADMIN rows \u2014 tiebreaker is MIN(id).
        long otherFirstId = insertWorkspaceUser(workspaceId, otherUserId, ADMIN_ORDINAL);
        long otherSecondId = insertWorkspaceUser(workspaceId, otherUserId, ADMIN_ORDINAL);

        assertThat(otherFirstId).isLessThan(otherSecondId);

        rerunChangelog(UNIQUE_CONSTRAINT_CHANGELOG, UNIQUE_CONSTRAINT_CHANGESET_ID);

        // User 100: the ADMIN row (id=secondId) survives \u2014 role preservation takes precedence over id ordering.
        Long survivingId = jdbc.queryForObject(
            "SELECT id FROM workspace_user WHERE workspace_id = ? AND user_id = ?",
            Long.class, workspaceId, userId);

        assertThat(survivingId)
            .as("Dedup must keep the ADMIN row, not the MIN(id) row \u2014 otherwise migration silently demotes "
                + "users from ADMIN to EDITOR when their duplicate history contains mixed roles")
            .isEqualTo(secondId);

        Integer survivingRole = jdbc.queryForObject(
            "SELECT workspace_role FROM workspace_user WHERE workspace_id = ? AND user_id = ?",
            Integer.class, workspaceId, userId);

        assertThat(survivingRole).isEqualTo(ADMIN_ORDINAL);

        // User 200: the MIN(id) row wins the tiebreaker between two equal-role rows.
        Long otherSurvivingId = jdbc.queryForObject(
            "SELECT id FROM workspace_user WHERE workspace_id = ? AND user_id = ?",
            Long.class, workspaceId, otherUserId);

        assertThat(otherSurvivingId).isEqualTo(otherFirstId);

        // Constraint registered after dedup.
        Long constraintCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_name = 'workspace_user' AND constraint_type = 'UNIQUE' "
                + "AND constraint_name = 'uk_workspace_user_workspace_user'",
            Long.class);

        assertThat(constraintCount).isEqualTo(1L);
    }

    /**
     * Removes the {@code databasechangelog} bookkeeping row for the given changeset and re-runs the changelog file via
     * the Liquibase Java API. Mirrors what an operator does with {@code clearCheckSums}-then-{@code update}, but scoped
     * to a single changeset so other migrations in the changelog file (or globally) are not affected.
     */
    private void rerunChangelog(String changelogResource, String changesetId) throws Exception {
        jdbc.update(
            "DELETE FROM databasechangelog WHERE id = ? AND filename = ?",
            changesetId, changelogResource);

        try (var connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            try (Liquibase liquibase = new Liquibase(
                changelogResource, new ClassLoaderResourceAccessor(), database)) {

                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }

    private long insertWorkspace(String name) {
        return jdbc.queryForObject(
            "INSERT INTO workspace (name, description, created_date, created_by, "
                + "last_modified_date, last_modified_by, version) "
                + "VALUES (?, NULL, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0) RETURNING id",
            Long.class, name);
    }

    private long insertProject(String name, long workspaceId) {
        return jdbc.queryForObject(
            "INSERT INTO project (name, description, workspace_id, uuid, "
                + "created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, NULL, ?, gen_random_uuid(), "
                + "CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0) RETURNING id",
            Long.class, name, workspaceId);
    }

    private long insertWorkspaceUser(long workspaceId, long userId, int workspaceRole) {
        return jdbc.queryForObject(
            "INSERT INTO workspace_user (workspace_id, user_id, workspace_role, "
                + "created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0) RETURNING id",
            Long.class, workspaceId, userId, workspaceRole);
    }

    private Integer workspaceRoleOf(long workspaceId, long userId) {
        return jdbc.queryForObject(
            "SELECT workspace_role FROM workspace_user WHERE workspace_id = ? AND user_id = ?",
            Integer.class, workspaceId, userId);
    }

    private Integer projectRoleOf(long projectId, long userId) {
        return jdbc.queryForObject(
            "SELECT project_role FROM project_user WHERE project_id = ? AND user_id = ?",
            Integer.class, projectId, userId);
    }

    private long projectUserCount() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM project_user", Long.class);

        return count == null ? 0L : count;
    }
}

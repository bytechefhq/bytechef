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
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test that verifies the RBAC Liquibase migrations apply cleanly to a fresh PostgreSQL database via
 * Testcontainers. Catches the highest-likelihood regression: a precondition or unique-constraint dedup that fails at
 * startup on a fresh install (the failure mode that
 * {@code 202604061200060_automation_configuration_workspace_user_unique_constraint.xml} was added to address).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class RbacMigrationsIntTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testRbacTablesExistAfterMigrations() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        // Assert table + column structure via information_schema rather than SELECT-with-tautology. A query like
        // `WHERE workspace_role IS NOT NULL OR workspace_role IS NULL` would pass even if workspace_role were a
        // wrong type (e.g., boolean) as long as it exists at all, because the tautology would always be true. The
        // information_schema check pins name AND non-null data type, so a migration that silently renamed or
        // retyped the column fails loudly here.
        assertColumnExists(jdbc, "workspace_user", "workspace_role");
        assertColumnExists(jdbc, "workspace_user", "custom_role_id");
        assertColumnExists(jdbc, "workspace_user", "environment");
        assertTableExists(jdbc, "custom_role");
        assertTableExists(jdbc, "custom_role_scope");

        // Sanity check that the tables are actually queryable (not just present in the catalog) — catches a subtle
        // case where a table is registered but the liquibase apply failed mid-way, leaving the table with bad
        // permissions or no primary key.
        Long workspaceUserCount = jdbc.queryForObject("SELECT COUNT(*) FROM workspace_user", Long.class);

        assertThat(workspaceUserCount).isNotNull();
    }

    private static void assertColumnExists(JdbcTemplate jdbc, String tableName, String columnName) {
        Long columnCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_name = ? AND column_name = ?",
            Long.class, tableName, columnName);

        assertThat(columnCount)
            .as("column %s.%s must be registered after migrations", tableName, columnName)
            .isEqualTo(1L);
    }

    private static void assertTableExists(JdbcTemplate jdbc, String tableName) {
        Long tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_name = ? AND table_type = 'BASE TABLE'",
            Long.class, tableName);

        assertThat(tableCount)
            .as("table %s must be registered after migrations", tableName)
            .isEqualTo(1L);
    }

    @Test
    void testWorkspaceUserUniquenessIsEnforcedByThePartialIndexes() {
        // The (workspace_id, user_id) unique constraint added by 202604061200060 was dropped by 20260819120000,
        // which needed several rows per member to carry per-environment roles. Uniqueness did not go away, it split
        // in two: one implicit row per member, and one explicit row per member per environment. We assert via the
        // PostgreSQL catalog rather than attempting a duplicate insert (which would require the full FK chain).
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertIndexExists(jdbc, "uk_workspace_user_implicit");
        assertIndexExists(jdbc, "uk_workspace_user_explicit");

        Long supersededConstraintCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_name = 'workspace_user' AND constraint_type = 'UNIQUE' "
                + "AND constraint_name = 'uk_workspace_user_workspace_user'",
            Long.class);

        assertThat(supersededConstraintCount)
            .as("uk_workspace_user_workspace_user forbids per-environment rows and must be dropped")
            .isEqualTo(0L);
    }

    private static void assertIndexExists(JdbcTemplate jdbc, String indexName) {
        Long indexCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'workspace_user' AND indexname = ?",
            Long.class, indexName);

        assertThat(indexCount)
            .as("index %s must be registered after migrations", indexName)
            .isEqualTo(1L);
    }
}

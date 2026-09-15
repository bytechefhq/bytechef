/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the per-environment shape of {@code workspace_user} against a real PostgreSQL: the {@code environment}
 * column exists, the two partial unique indexes hold, and no unique constraint forbids several rows per member.
 * <p>
 * Rows are inserted through raw SQL rather than the repository so that the assertions are about the schema alone. The
 * {@code workspace_id} foreign key is unconditional and so a real {@code workspace} row is seeded per test; the
 * {@code user_id} foreign key is declared {@code contextFilter="mono"} and is therefore not applied under the
 * {@code configuration} Liquibase context this profile activates.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class PerEnvironmentRoleIntTest {

    private static final int DEVELOPMENT = 0;
    private static final int STAGING = 1;
    private static final int PRODUCTION = 2;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void testAllowsOneImplicitRowPerMember() {
        long workspaceId = insertWorkspace("implicit");

        insertMember(1L, workspaceId, null);

        assertThatThrownBy(() -> insertMember(1L, workspaceId, null)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAllowsOneRowPerEnvironment() {
        long workspaceId = insertWorkspace("per-environment");

        insertMember(2L, workspaceId, DEVELOPMENT);
        insertMember(2L, workspaceId, STAGING);

        assertThatThrownBy(() -> insertMember(2L, workspaceId, DEVELOPMENT)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testAllowsImplicitAndExplicitRowsToCoexistInTheDatabase() {
        long workspaceId = insertWorkspace("coexistence");

        insertMember(3L, workspaceId, null);

        // The database permits the combination, because a partial index cannot express "not both modes". Exactly one
        // mode at a time is a service-layer invariant enforced by WorkspaceUserServiceImpl, not by the schema. This
        // asserts that the constraint is intentionally weaker than the rule.
        assertThatCode(() -> insertMember(3L, workspaceId, PRODUCTION)).doesNotThrowAnyException();
    }

    @Test
    void testWorkspaceUserHasNoUniqueConstraint() {
        Long constraintCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_schema = current_schema() AND table_name = 'workspace_user' "
                + "AND constraint_type = 'UNIQUE'",
            Long.class);

        assertThat(constraintCount)
            .as("a unique constraint would forbid per-environment rows; the partial indexes hold uniqueness")
            .isEqualTo(0L);
    }

    @Test
    void testBothPartialUniqueIndexesAreRegistered() {
        assertThat(indexCount("uk_workspace_user_implicit"))
            .as("uk_workspace_user_implicit must be registered after migrations")
            .isEqualTo(1L);
        assertThat(indexCount("uk_workspace_user_explicit"))
            .as("uk_workspace_user_explicit must be registered after migrations")
            .isEqualTo(1L);
    }

    @Test
    void testImplicitFinderIgnoresEnvironmentRows() {
        long workspaceId = insertWorkspace("implicit-finder");

        workspaceUserRepository.save(WorkspaceUser.forRole(10L, workspaceId, WorkspaceRole.EDITOR, null));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(10L, workspaceId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        Optional<WorkspaceUser> implicitRow =
            workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(10L, workspaceId);

        assertThat(implicitRow).isPresent();
        assertThat(implicitRow.get()
            .getEnvironment()).isNull();
    }

    @Test
    void testEnvironmentFinderReturnsOnlyThatEnvironment() {
        long workspaceId = insertWorkspace("environment-finder");

        workspaceUserRepository.save(
            WorkspaceUser.forRole(11L, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));

        assertThat(
            workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
                11L, workspaceId, Environment.DEVELOPMENT.ordinal()))
                    .isPresent();
        assertThat(
            workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
                11L, workspaceId, Environment.PRODUCTION.ordinal()))
                    .isEmpty();
    }

    @Test
    void testListFinderReturnsEveryRowForTheMember() {
        long workspaceId = insertWorkspace("list-finder");

        workspaceUserRepository.save(
            WorkspaceUser.forRole(12L, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(12L, workspaceId, WorkspaceRole.VIEWER, Environment.STAGING));

        assertThat(workspaceUserRepository.findAllByUserIdAndWorkspaceId(12L, workspaceId)).hasSize(2);
    }

    @Test
    void testCountsImplicitAndExplicitAdminsPerEnvironment() {
        long workspaceId = insertWorkspace("admin-count");

        // An implicit admin administers every environment; an explicit admin only the one they name.
        workspaceUserRepository.save(WorkspaceUser.forRole(20L, workspaceId, WorkspaceRole.ADMIN));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(21L, workspaceId, WorkspaceRole.ADMIN, Environment.PRODUCTION));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(22L, workspaceId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        assertThat(
            workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), Environment.PRODUCTION.ordinal()))
                    .as("the implicit admin plus the Production admin, but not the Production viewer")
                    .isEqualTo(2L);
        assertThat(
            workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), Environment.STAGING.ordinal()))
                    .as("only the implicit admin reaches Staging")
                    .isEqualTo(1L);
    }

    @Test
    void testExistsFinderHoldsForAMemberWhoOnlyHoldsEnvironmentRows() {
        long workspaceId = insertWorkspace("exists-finder");

        // The membership question, and the reason it is not an Optional-returning finder: two rows for one member would
        // make a single-result derived query throw IncorrectResultSizeDataAccessException against this very schema.
        workspaceUserRepository.save(
            WorkspaceUser.forRole(13L, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(13L, workspaceId, WorkspaceRole.VIEWER, Environment.PRODUCTION));

        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceId(13L, workspaceId))
            .as("a member holding only environment rows is still a member")
            .isTrue();
        assertThat(workspaceUserRepository.existsByUserIdAndWorkspaceId(14L, workspaceId))
            .as("and somebody with no row at all is not")
            .isFalse();
    }

    @Test
    void testCountsOneAdminHoldingEveryEnvironmentAsOneAdmin() {
        long workspaceId = insertWorkspace("distinct-admin-count");

        // The workspace's only admin, in explicit mode with ADMIN everywhere. Counted as rows they read as three
        // admins,
        // which would let the last-admin guard wave through the removal that empties the workspace of admins.
        workspaceUserRepository.save(
            WorkspaceUser.forRole(30L, workspaceId, WorkspaceRole.ADMIN, Environment.DEVELOPMENT));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(30L, workspaceId, WorkspaceRole.ADMIN, Environment.STAGING));
        workspaceUserRepository.save(
            WorkspaceUser.forRole(30L, workspaceId, WorkspaceRole.ADMIN, Environment.PRODUCTION));

        assertThat(
            workspaceUserRepository.countDistinctMembersWithWorkspaceRole(workspaceId, WorkspaceRole.ADMIN.ordinal()))
                .as("three ADMIN rows held by one person are one admin, not three")
                .isEqualTo(1L);

        workspaceUserRepository.save(WorkspaceUser.forRole(31L, workspaceId, WorkspaceRole.ADMIN));

        assertThat(
            workspaceUserRepository.countDistinctMembersWithWorkspaceRole(workspaceId, WorkspaceRole.ADMIN.ordinal()))
                .as("a second admin, implicit this time, makes two")
                .isEqualTo(2L);
    }

    private Long indexCount(String indexName) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'workspace_user' AND indexname = ?", Long.class,
            indexName);
    }

    private long insertWorkspace(String name) {
        // workspace.name carries a unique constraint, so each test seeds its own workspace under its own name.
        Long workspaceId = jdbcTemplate.queryForObject(
            "INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version) "
                + "VALUES (?, NOW(), 'test', NOW(), 'test', 0) RETURNING id",
            Long.class, "per-environment-role-test-" + name);

        assertThat(workspaceId).isNotNull();

        return workspaceId;
    }

    private void insertMember(long userId, long workspaceId, Integer environment) {
        jdbcTemplate.update(
            "INSERT INTO workspace_user "
                + "(user_id, workspace_id, workspace_role, environment, created_date, created_by, "
                + "last_modified_date, last_modified_by, version) "
                + "VALUES (?, ?, 1, ?, NOW(), 'test', NOW(), 'test', 0)",
            userId, workspaceId, environment);
    }
}

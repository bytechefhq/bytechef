/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.ee.automation.configuration.config.EeAutomationConfigurationIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the SQL semantics of the cascade-related repository methods against PostgreSQL via Testcontainers. The unit
 * test {@code WorkspaceUserServiceTest#testRemoveWorkspaceUserCascadesProjectMembershipsAndEvictsCache} only verifies
 * the service wires the calls correctly — this test verifies the actual JOIN-based SQL deletes the right rows, leaves
 * neighboring rows untouched, and that the snapshot query returns the project ids before deletion.
 *
 * <p>
 * This catches the class of bug Critical #1 was introduced by: a typo in the derived query name, a missing JOIN
 * predicate, or a stale {@code findAllByUserId} (which would mock-pass but DB-fail).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = EeAutomationConfigurationIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class ProjectUserRepositoryIntTest {

    @Autowired
    private ProjectUserRepository projectUserRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private CustomRoleRepository customRoleRepository;

    private Workspace workspaceA;
    private Workspace workspaceB;
    private Project projectInA;
    private Project anotherProjectInA;
    private Project projectInB;

    @BeforeEach
    void setUp() {
        workspaceA = workspaceRepository.save(new Workspace("ws-A"));
        workspaceB = workspaceRepository.save(new Workspace("ws-B"));

        projectInA = projectRepository.save(buildProject("project A1", workspaceA.getId()));
        anotherProjectInA = projectRepository.save(buildProject("project A2", workspaceA.getId()));
        projectInB = projectRepository.save(buildProject("project B1", workspaceB.getId()));
    }

    @AfterEach
    void tearDown() {
        projectUserRepository.deleteAll();
        customRoleRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testFindProjectIdsByUserIdAndWorkspaceIdReturnsOnlyMatchingMemberships() {
        long userId = 100L;
        long otherUserId = 200L;

        // userId is a member of two projects in workspaceA and one in workspaceB. The query must return exactly
        // workspaceA's two projects — workspaceB's project must NOT leak in (different workspace), and otherUser's
        // memberships must NOT be returned (different user).
        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), userId, ProjectRole.EDITOR));
        projectUserRepository.save(ProjectUser.forBuiltInRole(anotherProjectInA.getId(), userId, ProjectRole.VIEWER));
        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInB.getId(), userId, ProjectRole.ADMIN));
        projectUserRepository.save(
            ProjectUser.forBuiltInRole(projectInA.getId(), otherUserId, ProjectRole.ADMIN));

        List<Long> projectIds = projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(
            userId, workspaceA.getId());

        assertThat(projectIds).containsExactlyInAnyOrder(projectInA.getId(), anotherProjectInA.getId());
    }

    @Test
    void testFindProjectIdsByUserIdAndWorkspaceIdReturnsEmptyForUnknownUser() {
        // No memberships at all for this user — should return empty, not null.
        List<Long> projectIds = projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(
            999L, workspaceA.getId());

        assertThat(projectIds).isEmpty();
    }

    @Test
    void testDeleteByUserIdAndWorkspaceIdDeletesOnlyMatchingRows() {
        long userId = 100L;
        long otherUserId = 200L;

        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), userId, ProjectRole.EDITOR));
        projectUserRepository.save(ProjectUser.forBuiltInRole(anotherProjectInA.getId(), userId, ProjectRole.VIEWER));
        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInB.getId(), userId, ProjectRole.ADMIN));
        projectUserRepository.save(
            ProjectUser.forBuiltInRole(projectInA.getId(), otherUserId, ProjectRole.ADMIN));

        int deleted = projectUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceA.getId());

        // Only the two rows in workspaceA for userId should have been removed (2 deletions).
        assertThat(deleted).isEqualTo(2);

        // userId's workspaceB membership must survive — different workspace, untouched.
        assertThat(projectUserRepository.findByProjectIdAndUserId(projectInB.getId(), userId)).isPresent();

        // otherUserId's membership in workspaceA must survive — same workspace, different user.
        assertThat(projectUserRepository.findByProjectIdAndUserId(projectInA.getId(), otherUserId)).isPresent();

        // userId's memberships in workspaceA are gone.
        assertThat(projectUserRepository.findByProjectIdAndUserId(projectInA.getId(), userId)).isEmpty();
        assertThat(projectUserRepository.findByProjectIdAndUserId(anotherProjectInA.getId(), userId)).isEmpty();
    }

    @Test
    void testDeleteByUserIdAndWorkspaceIdReturnsZeroWhenNoRowsMatch() {
        // Deleting for a user with no memberships in the target workspace must be a safe no-op.
        int deleted = projectUserRepository.deleteByUserIdAndWorkspaceId(999L, workspaceA.getId());

        assertThat(deleted).isZero();
    }

    @Test
    void testCascadeOrderingHazardFindBeforeDeleteSeesRowsAndFindAfterDeleteDoesNot() {
        // Reproduces the original Critical #1 hazard: querying AFTER the delete returns empty, so callers must
        // snapshot BEFORE the delete to know which cache entries to evict. This test pins that contract.
        long userId = 100L;

        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), userId, ProjectRole.EDITOR));

        List<Long> beforeSnapshot = projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(
            userId, workspaceA.getId());

        assertThat(beforeSnapshot).containsExactly(projectInA.getId());

        projectUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceA.getId());

        List<Long> afterSnapshot = projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(
            userId, workspaceA.getId());

        assertThat(afterSnapshot)
            .as("Querying after the cascade delete returns empty — callers must snapshot BEFORE the delete to "
                + "know which cache keys to evict (see WorkspaceUserServiceImpl#removeWorkspaceUser)")
            .isEmpty();
    }

    @Test
    void testCountEffectiveAdminsCountsBuiltInAdmins() {
        // Two built-in ADMINs and one EDITOR on the same project. countEffectiveAdmins must return 2.
        long adminOneId = 100L;
        long adminTwoId = 200L;
        long editorId = 300L;

        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), adminOneId, ProjectRole.ADMIN));
        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), adminTwoId, ProjectRole.ADMIN));
        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), editorId, ProjectRole.EDITOR));

        long count = projectUserRepository.countEffectiveAdmins(
            projectInA.getId(), ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void testCountEffectiveAdminsIncludesCustomRoleAdmins() {
        // One built-in ADMIN plus one custom-role member whose role grants PROJECT_MANAGE_USERS. The JOIN must
        // count both. Without the scope-granting custom role, the last-admin guard would incorrectly allow the
        // built-in ADMIN to be removed, bricking the project.
        CustomRole managerRole = customRoleRepository.save(
            new CustomRole("Project Manager", Set.of(PermissionScope.PROJECT_MANAGE_USERS)));

        long builtInAdminId = 100L;
        long customRoleAdminId = 200L;

        projectUserRepository.save(
            ProjectUser.forBuiltInRole(projectInA.getId(), builtInAdminId, ProjectRole.ADMIN));
        projectUserRepository.save(
            ProjectUser.forCustomRole(projectInA.getId(), customRoleAdminId, managerRole.getId()));

        long count = projectUserRepository.countEffectiveAdmins(
            projectInA.getId(), ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void testCountEffectiveAdminsExcludesCustomRoleWithoutAdminScope() {
        // A custom role that does NOT grant PROJECT_MANAGE_USERS must not be counted. Only the built-in ADMIN row
        // should count, so removing the ADMIN would correctly trigger the last-admin guard.
        CustomRole viewerLikeRole = customRoleRepository.save(
            new CustomRole("Read-only", Set.of(PermissionScope.WORKFLOW_VIEW)));

        long builtInAdminId = 100L;
        long customRoleMemberId = 200L;

        projectUserRepository.save(
            ProjectUser.forBuiltInRole(projectInA.getId(), builtInAdminId, ProjectRole.ADMIN));
        projectUserRepository.save(
            ProjectUser.forCustomRole(projectInA.getId(), customRoleMemberId, viewerLikeRole.getId()));

        long count = projectUserRepository.countEffectiveAdmins(
            projectInA.getId(), ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void testCountEffectiveAdminsScopedToProject() {
        // Admins in a neighboring project must not leak into this project's count. Catches a missing
        // WHERE project_id predicate.
        projectUserRepository.save(ProjectUser.forBuiltInRole(projectInA.getId(), 100L, ProjectRole.ADMIN));
        projectUserRepository.save(ProjectUser.forBuiltInRole(anotherProjectInA.getId(), 200L, ProjectRole.ADMIN));
        projectUserRepository.save(ProjectUser.forBuiltInRole(anotherProjectInA.getId(), 300L, ProjectRole.ADMIN));

        long countA = projectUserRepository.countEffectiveAdmins(
            projectInA.getId(), ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());
        long countAnotherA = projectUserRepository.countEffectiveAdmins(
            anotherProjectInA.getId(), ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        assertThat(countA).isEqualTo(1L);
        assertThat(countAnotherA).isEqualTo(2L);
    }

    @Test
    void testCountEffectiveAdminsReturnsZeroForProjectWithoutMembers() {
        // Defensive: an orphan project with no memberships yields 0 without NPE or NULL-propagation.
        long count = projectUserRepository.countEffectiveAdmins(
            projectInB.getId(), ProjectRole.ADMIN.ordinal(), PermissionScope.PROJECT_MANAGE_USERS.name());

        assertThat(count).isZero();
    }

    private Project buildProject(String name, Long workspaceId) {
        Project project = Project.builder()
            .name(name)
            .description("ProjectUserRepositoryIntTest fixture")
            .workspaceId(workspaceId)
            .build();

        // The uuid column is NOT NULL but the builder does not set it; assign a random UUID per row so the insert
        // succeeds. Production code paths (ProjectFacadeImpl) generate the uuid before save.
        project.setUuid(UUID.randomUUID());

        return project;
    }
}

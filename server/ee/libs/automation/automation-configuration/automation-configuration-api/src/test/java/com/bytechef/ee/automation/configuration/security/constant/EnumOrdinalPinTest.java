/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the persisted ordinals of {@link ProjectRole}, {@link WorkspaceRole}, and {@link PermissionScope}, plus the
 * privilege ranks of the two role enums.
 *
 * <p>
 * {@code ProjectRole} and {@code WorkspaceRole} are stored as INT columns ({@code project_user.project_role},
 * {@code workspace_user.workspace_role}); reordering their constants silently corrupts every existing row. New
 * constants must be appended, never inserted. Privilege ranks are decoupled from ordinals so a new role can sit
 * anywhere in the hierarchy without moving its declaration position; the rank-to-role mapping is pinned below.
 *
 * <p>
 * {@code PermissionScope} persists by name into {@code custom_role_scope.scope} (VARCHAR(50)), so its ordinals are NOT
 * load-bearing for storage. Ordinals are still pinned because the size pin catches accidental insertions in the wrong
 * spot, and the per-constant ordinals make intentional reordering a visible edit.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalPinTest {

    @Test
    void testProjectRoleOrdinalsPinned() {
        assertThat(ProjectRole.ADMIN.ordinal()).isEqualTo(0);
        assertThat(ProjectRole.EDITOR.ordinal()).isEqualTo(1);
        assertThat(ProjectRole.OPERATOR.ordinal()).isEqualTo(2);
        assertThat(ProjectRole.VIEWER.ordinal()).isEqualTo(3);
        assertThat(ProjectRole.values()).hasSize(4);
    }

    @Test
    void testProjectRolePrivilegeRanksPinned() {
        assertThat(ProjectRole.ADMIN.getPrivilegeRank()).isEqualTo(0);
        assertThat(ProjectRole.EDITOR.getPrivilegeRank()).isEqualTo(1);
        assertThat(ProjectRole.OPERATOR.getPrivilegeRank()).isEqualTo(2);
        assertThat(ProjectRole.VIEWER.getPrivilegeRank()).isEqualTo(3);
    }

    @Test
    void testWorkspaceRoleOrdinalsPinned() {
        assertThat(WorkspaceRole.ADMIN.ordinal()).isEqualTo(0);
        assertThat(WorkspaceRole.EDITOR.ordinal()).isEqualTo(1);
        assertThat(WorkspaceRole.VIEWER.ordinal()).isEqualTo(2);
        assertThat(WorkspaceRole.values()).hasSize(3);
    }

    @Test
    void testWorkspaceRolePrivilegeRanksPinned() {
        assertThat(WorkspaceRole.ADMIN.getPrivilegeRank()).isEqualTo(0);
        assertThat(WorkspaceRole.EDITOR.getPrivilegeRank()).isEqualTo(1);
        assertThat(WorkspaceRole.VIEWER.getPrivilegeRank()).isEqualTo(2);
    }

    @Test
    void testPermissionScopeOrdinalsPinned() {
        assertThat(PermissionScope.WORKFLOW_VIEW.ordinal()).isEqualTo(0);
        assertThat(PermissionScope.WORKFLOW_CREATE.ordinal()).isEqualTo(1);
        assertThat(PermissionScope.WORKFLOW_EDIT.ordinal()).isEqualTo(2);
        assertThat(PermissionScope.WORKFLOW_DELETE.ordinal()).isEqualTo(3);
        assertThat(PermissionScope.WORKFLOW_TOGGLE.ordinal()).isEqualTo(4);

        assertThat(PermissionScope.EXECUTION_VIEW.ordinal()).isEqualTo(5);
        assertThat(PermissionScope.EXECUTION_DATA.ordinal()).isEqualTo(6);
        assertThat(PermissionScope.EXECUTION_RETRY.ordinal()).isEqualTo(7);

        assertThat(PermissionScope.CONNECTION_VIEW.ordinal()).isEqualTo(8);
        assertThat(PermissionScope.CONNECTION_CREATE.ordinal()).isEqualTo(9);
        assertThat(PermissionScope.CONNECTION_EDIT.ordinal()).isEqualTo(10);
        assertThat(PermissionScope.CONNECTION_DELETE.ordinal()).isEqualTo(11);
        assertThat(PermissionScope.CONNECTION_USE.ordinal()).isEqualTo(12);

        assertThat(PermissionScope.AGENT_VIEW.ordinal()).isEqualTo(13);
        assertThat(PermissionScope.AGENT_CREATE.ordinal()).isEqualTo(14);
        assertThat(PermissionScope.AGENT_EDIT.ordinal()).isEqualTo(15);
        assertThat(PermissionScope.AGENT_EXECUTE.ordinal()).isEqualTo(16);

        assertThat(PermissionScope.PROJECT_VIEW_USERS.ordinal()).isEqualTo(17);
        assertThat(PermissionScope.PROJECT_MANAGE_USERS.ordinal()).isEqualTo(18);
        assertThat(PermissionScope.PROJECT_SETTINGS.ordinal()).isEqualTo(19);

        assertThat(PermissionScope.DEPLOYMENT_PUSH.ordinal()).isEqualTo(20);
        assertThat(PermissionScope.DEPLOYMENT_PULL.ordinal()).isEqualTo(21);

        assertThat(PermissionScope.PROJECT_DELETE.ordinal()).isEqualTo(22);

        // Hard-coded length so adding a new constant in the wrong position is caught here even if all the existing
        // ordinals still pass.
        assertThat(PermissionScope.values()).hasSize(23);
    }
}

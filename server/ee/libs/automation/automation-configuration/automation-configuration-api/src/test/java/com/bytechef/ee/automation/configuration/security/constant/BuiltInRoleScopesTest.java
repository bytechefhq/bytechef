/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pins the documented invariant that built-in role scopes form a strict subset chain (VIEWER &sub; OPERATOR &sub;
 * EDITOR &sub; ADMIN). Without this test, a future scope addition could grant something to OPERATOR but not EDITOR (or
 * to VIEWER but not OPERATOR) and the application would silently behave inconsistently.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class BuiltInRoleScopesTest {

    @Test
    void testAdminGetsEveryDefinedScope() {
        assertThat(BuiltInRoleScopes.getScopesForRole(ProjectRole.ADMIN))
            .hasSize(PermissionScope.values().length)
            .containsExactlyInAnyOrder(PermissionScope.values());
    }

    @Test
    void testOperatorIsSupersetOfViewer() {
        Set<PermissionScope> viewer = BuiltInRoleScopes.getScopesForRole(ProjectRole.VIEWER);
        Set<PermissionScope> operator = BuiltInRoleScopes.getScopesForRole(ProjectRole.OPERATOR);

        assertThat(operator).containsAll(viewer);
        assertThat(operator).hasSizeGreaterThanOrEqualTo(viewer.size());
    }

    @Test
    void testEditorIsSupersetOfOperator() {
        Set<PermissionScope> operator = BuiltInRoleScopes.getScopesForRole(ProjectRole.OPERATOR);
        Set<PermissionScope> editor = BuiltInRoleScopes.getScopesForRole(ProjectRole.EDITOR);

        assertThat(editor).containsAll(operator);
        assertThat(editor).hasSizeGreaterThanOrEqualTo(operator.size());
    }

    @Test
    void testAdminIsSupersetOfEditor() {
        Set<PermissionScope> editor = BuiltInRoleScopes.getScopesForRole(ProjectRole.EDITOR);
        Set<PermissionScope> admin = BuiltInRoleScopes.getScopesForRole(ProjectRole.ADMIN);

        assertThat(admin).containsAll(editor);
    }

    @Test
    void testEditorDoesNotGrantUserManagement() {
        Set<PermissionScope> editor = BuiltInRoleScopes.getScopesForRole(ProjectRole.EDITOR);

        assertThat(editor).doesNotContain(
            PermissionScope.PROJECT_MANAGE_USERS,
            PermissionScope.PROJECT_SETTINGS,
            PermissionScope.PROJECT_DELETE);
    }

    @Test
    void testReturnedSetIsUnmodifiable() {
        Set<PermissionScope> viewer = BuiltInRoleScopes.getScopesForRole(ProjectRole.VIEWER);

        assertThatThrownBy(() -> viewer.add(PermissionScope.WORKFLOW_DELETE))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testNullRoleRejected() {
        assertThatThrownBy(() -> BuiltInRoleScopes.getScopesForRole(null))
            .isInstanceOf(NullPointerException.class);
    }
}

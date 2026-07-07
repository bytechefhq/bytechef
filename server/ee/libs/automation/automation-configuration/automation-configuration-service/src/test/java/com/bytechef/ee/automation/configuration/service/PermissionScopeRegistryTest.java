/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider.ScopeDefinition;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionScopeRegistryTest {

    private static final Set<String> EXPECTED_VIEWER_SCOPES = Set.of("ALPHA_VIEW", "BETA_VIEW");

    private static final Set<String> EDITOR_DELTA_SCOPES = Set.of("ALPHA_EDIT", "BETA_DELETE");

    private static final Set<String> ADMIN_DELTA_SCOPES = Set.of("ALPHA_MANAGE");

    // Synthetic providers exercise the registry's cross-provider aggregation and role tiering without coupling the test
    // to the production scope catalog (the real security.scope.* providers are contributed and verified separately).
    // Scopes are spread across all three tiers so the VIEWER ⊆ EDITOR ⊆ ADMIN invariant is actually exercised.
    private final PermissionScopeRegistry permissionScopeRegistry = new PermissionScopeRegistry(
        List.<PermissionScopeProvider>of(
            () -> Set.of(
                new ScopeDefinition(TestPermissionScope.ALPHA_VIEW, WorkspaceRole.VIEWER),
                new ScopeDefinition(TestPermissionScope.ALPHA_EDIT, WorkspaceRole.EDITOR),
                new ScopeDefinition(TestPermissionScope.ALPHA_MANAGE, WorkspaceRole.ADMIN)),
            () -> Set.of(
                new ScopeDefinition(TestPermissionScope.BETA_VIEW, WorkspaceRole.VIEWER),
                new ScopeDefinition(TestPermissionScope.BETA_DELETE, WorkspaceRole.EDITOR))));

    @Test
    void testBuiltInRoleTiers() {
        Set<String> expectedViewer = EXPECTED_VIEWER_SCOPES;
        Set<String> expectedEditor = union(expectedViewer, EDITOR_DELTA_SCOPES);
        Set<String> expectedAdmin = union(expectedEditor, ADMIN_DELTA_SCOPES);

        assertThat(permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER))
            .containsExactlyInAnyOrderElementsOf(expectedViewer);
        assertThat(permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR))
            .containsExactlyInAnyOrderElementsOf(expectedEditor);
        assertThat(permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN))
            .containsExactlyInAnyOrderElementsOf(expectedAdmin);
    }

    @Test
    void testAllScopeNames() {
        Set<String> expectedAll = union(union(EXPECTED_VIEWER_SCOPES, EDITOR_DELTA_SCOPES), ADMIN_DELTA_SCOPES);

        assertThat(permissionScopeRegistry.getAllScopeNames()).containsExactlyInAnyOrderElementsOf(expectedAll);
        assertThat(expectedAll).hasSize(5);
    }

    @Test
    void testViewerSubsetEditorSubsetAdmin() {
        Set<String> viewer = permissionScopeRegistry.getScopeNames(WorkspaceRole.VIEWER);
        Set<String> editor = permissionScopeRegistry.getScopeNames(WorkspaceRole.EDITOR);
        Set<String> admin = permissionScopeRegistry.getScopeNames(WorkspaceRole.ADMIN);

        assertThat(editor).containsAll(viewer);
        assertThat(admin).containsAll(editor);
    }

    @Test
    void testConflictingMinimumRoleFailsFast() {
        PermissionScopeProvider first =
            () -> Set.of(new ScopeDefinition(TestPermissionScope.X_SCOPE, WorkspaceRole.VIEWER));
        PermissionScopeProvider second =
            () -> Set.of(new ScopeDefinition(TestPermissionScope.X_SCOPE, WorkspaceRole.ADMIN));

        assertThatThrownBy(() -> new PermissionScopeRegistry(List.of(first, second)))
            .isInstanceOf(IllegalStateException.class);
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        return Stream.concat(first.stream(), second.stream())
            .collect(Collectors.toUnmodifiableSet());
    }

    // Synthetic scope enum kept local to the test so aggregation/tiering is exercised without coupling to the
    // production scope catalog (the real security.scope.* enums are contributed and verified separately).
    private enum TestPermissionScope implements PermissionScopeType {

        ALPHA_VIEW,
        ALPHA_EDIT,
        ALPHA_MANAGE,
        BETA_VIEW,
        BETA_DELETE,
        X_SCOPE
    }
}

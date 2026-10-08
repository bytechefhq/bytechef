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
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
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
    void testBuiltInRolesReadLeastPrivilegedFirstAndCarryInheritedScopes() {
        // Read downwards, each tier shows what the one above adds — which only works if the scopes a role inherits are
        // included rather than only its delta. WorkspaceRole declares itself most-privileged-first, so this is not the
        // enum's own order and cannot be left to it.
        assertThat(permissionScopeRegistry.getBuiltInRoles())
            .extracting(BuiltInRoleDTO::name)
            .containsExactly("VIEWER", "EDITOR", "ADMIN");

        for (BuiltInRoleDTO builtInRole : permissionScopeRegistry.getBuiltInRoles()) {
            assertThat(builtInRole.scopes())
                .containsExactlyInAnyOrderElementsOf(
                    permissionScopeRegistry.getScopeNames(WorkspaceRole.valueOf(builtInRole.name())));
        }
    }

    @Test
    void testScopeGroupsComeFromTheDeclaringEnum() {
        // A module owns an enum, not a provider — two providers contributing constants of the same enum are one
        // module, and a module's own second enum would be its own group. Grouping on the provider instead would let
        // an EE/CE split of the same module render as two headings for one thing.
        PermissionScopeRegistry registry = new PermissionScopeRegistry(
            List.<PermissionScopeProvider>of(
                () -> Set.of(new ScopeDefinition(TestPermissionScope.ALPHA_EDIT, WorkspaceRole.EDITOR)),
                () -> Set.of(new ScopeDefinition(TestPermissionScope.ALPHA_VIEW, WorkspaceRole.VIEWER)),
                () -> Set.of(new ScopeDefinition(OtherModulePermissionScope.OTHER_VIEW, WorkspaceRole.VIEWER))));

        // Groups sorted by module and scopes in the enum's declaration order: the catalogue must read identically on
        // every boot, and the providers hand their scopes over in Set order, which is neither.
        assertThat(registry.getScopeGroups())
            .containsExactly(
                new PermissionScopeGroupDTO("OTHER_MODULE", List.of("OTHER_VIEW")),
                new PermissionScopeGroupDTO("TEST", List.of("ALPHA_VIEW", "ALPHA_EDIT")));
    }

    @Test
    void testScopeGroupsCoverEveryRegisteredScopeExactlyOnce() {
        List<String> groupedScopeNames = permissionScopeRegistry.getScopeGroups()
            .stream()
            .flatMap(scopeGroup -> scopeGroup.scopes()
                .stream())
            .toList();

        // A scope missing from the grouped view is a permission no operator can grant through the editor, while a
        // duplicated one is a checkbox that appears twice under different headings.
        assertThat(groupedScopeNames).doesNotHaveDuplicates();
        assertThat(groupedScopeNames)
            .containsExactlyInAnyOrderElementsOf(permissionScopeRegistry.getAllScopeNames());
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

    // A second enum, so the grouping is pinned to the declaring enum rather than to the provider that hands it over.
    private enum OtherModulePermissionScope implements PermissionScopeType {

        OTHER_VIEW
    }
}

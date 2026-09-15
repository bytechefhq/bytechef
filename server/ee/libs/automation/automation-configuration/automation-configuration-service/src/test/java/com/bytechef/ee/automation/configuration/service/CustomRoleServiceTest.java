/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.CustomRoleScope;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomRoleServiceTest {

    private CustomRoleRepository customRoleRepository;
    private PermissionScopeRegistry permissionScopeRegistry;
    private PermissionService permissionService;
    private WorkspaceUserService workspaceUserService;
    private CustomRoleServiceImpl customRoleService;

    @BeforeEach
    void setUp() {
        customRoleRepository = mock(CustomRoleRepository.class);
        permissionScopeRegistry = mock(PermissionScopeRegistry.class);
        permissionService = mock(PermissionService.class);
        workspaceUserService = mock(WorkspaceUserService.class);

        when(permissionScopeRegistry.getAllScopeNames())
            .thenReturn(Set.of("WORKFLOW_VIEW", "WORKFLOW_CREATE", "WORKFLOW_EDIT"));

        customRoleService = new CustomRoleServiceImpl(customRoleRepository, permissionScopeRegistry, permissionService,
            workspaceUserService);
    }

    @Test
    void testCreateCustomRolePersistsCorrectScopes() {
        Set<String> scopeNames = Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT");

        // The saved instance is echoed back rather than replaced by a pre-built one. Stubbing save to return an object
        // the test itself constructed made the assertions below describe that object and not the service: a role built
        // with the wrong scopes, or with none, passed all the same.
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomRole result = customRoleService.createCustomRole(
            "Custom Editor", "Can view and edit workflows", scopeNames);

        assertThat(result.getName()).isEqualTo("Custom Editor");
        assertThat(result.getDescription()).isEqualTo("Can view and edit workflows");
        assertThat(result.getScopes()).hasSize(2);
        assertThat(result.getScopes()).extracting(CustomRoleScope::scope)
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_EDIT");

        // Captured as well as echoed, so the row handed to the repository is asserted and not only the value returned.
        ArgumentCaptor<CustomRole> customRoleArgumentCaptor = ArgumentCaptor.forClass(CustomRole.class);

        verify(customRoleRepository).save(customRoleArgumentCaptor.capture());

        assertThat(
            customRoleArgumentCaptor.getValue()
                .getScopeNames())
                    .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_EDIT");
    }

    @Test
    void testCreateCustomRoleRejectsUnknownScope() {
        assertThatThrownBy(
            () -> customRoleService.createCustomRole("Bad Role", "desc", Set.of("NOT_A_REAL_SCOPE")))
                .isInstanceOf(ConfigurationException.class);

        verify(customRoleRepository, never()).save(any(CustomRole.class));
    }

    @Test
    void testDeleteCustomRoleInUseThrowsException() {
        when(customRoleRepository.existsById(1L)).thenReturn(true);
        when(workspaceUserService.countByCustomRoleId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> customRoleService.deleteCustomRole(1L))
            .isInstanceOf(ConfigurationException.class);

        verify(customRoleRepository, never()).deleteById(1L);
    }

    @Test
    void testDeleteCustomRoleNotInUseSucceeds() {
        when(customRoleRepository.existsById(1L)).thenReturn(true);
        when(workspaceUserService.countByCustomRoleId(1L)).thenReturn(0L);

        customRoleService.deleteCustomRole(1L);

        verify(customRoleRepository).deleteById(1L);
    }

    @Test
    void testGetBuiltInRolesComesFromTheRegistry() {
        // The same registry the authorization checks read their tiers from — a separately maintained list would
        // describe permissions the server stopped granting.
        when(permissionScopeRegistry.getBuiltInRoles())
            .thenReturn(List.of(new BuiltInRoleDTO("VIEWER", List.of("WORKFLOW_VIEW"))));

        assertThat(customRoleService.getBuiltInRoles())
            .containsExactly(new BuiltInRoleDTO("VIEWER", List.of("WORKFLOW_VIEW")));
    }

    @Test
    void testGetPermissionScopeGroupsComesFromTheRegistry() {
        // The same registry the write path validates against — a client editor built from a different list would
        // either offer names the server rejects or omit ones a module added later.
        when(permissionScopeRegistry.getScopeGroups())
            .thenReturn(
                List.of(
                    new PermissionScopeGroupDTO("WORKFLOW", List.of("WORKFLOW_VIEW", "WORKFLOW_CREATE",
                        "WORKFLOW_EDIT"))));

        assertThat(customRoleService.getPermissionScopeGroups())
            .containsExactly(
                new PermissionScopeGroupDTO("WORKFLOW", List.of("WORKFLOW_VIEW", "WORKFLOW_CREATE", "WORKFLOW_EDIT")));
    }

    @Test
    void testUpdateCustomRoleReplacesScopes() {
        CustomRole existingRole = new CustomRole("Old Name", Set.of("WORKFLOW_VIEW"));

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<String> newScopeNames = Set.of("WORKFLOW_VIEW", "WORKFLOW_CREATE", "WORKFLOW_EDIT");

        CustomRole result =
            customRoleService.updateCustomRole(1L, "New Name", "Updated description", newScopeNames);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getScopes()).hasSize(3);

        verify(permissionService).evictAllWorkspaceScopeCache();
    }

    @Test
    void testUpdateCustomRoleEvictsCache() {
        CustomRole existingRole = new CustomRole("Role", Set.of("WORKFLOW_EDIT"));

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customRoleService.updateCustomRole(1L, "Role", "Desc", Set.of("WORKFLOW_VIEW"));

        verify(permissionService).evictAllWorkspaceScopeCache();
    }

    @Test
    void testUpdateCustomRoleSkipsCacheEvictionWhenScopesUnchanged() {
        // Name-only / description-only edits must NOT evict the global workspace scope cache. For tenants with
        // thousands of workspaces the eviction cost is meaningful, and no user's resolved permissions actually change.
        // If this assertion regresses, someone changed the scope-equality check in updateCustomRole and re-introduced
        // the perf cliff.
        Set<String> scopeNames = Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT");
        CustomRole existingRole = new CustomRole("Old Name", scopeNames);

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customRoleService.updateCustomRole(1L, "New Name", "New description", scopeNames);

        verify(permissionService, never()).evictAllWorkspaceScopeCache();
    }

    @Test
    void testGetCustomRolesForAWorkspaceReturnsEveryRole() {
        CustomRole role = new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"));

        when(customRoleRepository.findAll()).thenReturn(List.of(role));

        // The workspaceId is authorization context for the assignment picker, not a filter — every role is
        // tenant-global and assignable in any workspace.
        assertThat(customRoleService.getCustomRoles(7L)).containsExactly(role);
    }

    @Test
    void testGetCustomRolesWithoutWorkspaceReturnsEveryRole() {
        when(customRoleRepository.findAll()).thenReturn(List.of());

        customRoleService.getCustomRoles(null);

        // The tenant-wide view is a different authorization tier, guarded by isTenantAdmin() on the method.
        verify(customRoleRepository).findAll();
    }

    @Test
    void testDeleteCustomRoleRejectsAnUnknownId() {
        when(customRoleRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> customRoleService.deleteCustomRole(404L))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("does not exist");

        // deleteById on an unknown id is a silent no-op, so without the existence check this reported success and
        // published CUSTOM_ROLE_DELETED for a deletion that never happened.
        verify(customRoleRepository, never()).deleteById(404L);
    }

    @Test
    void testUpdateCustomRoleRejectsAnUnknownId() {
        when(customRoleRepository.findById(404L)).thenReturn(Optional.empty());

        // A typed ConfigurationException, not the bare NoSuchElementException OptionalUtils.get used to raise: that one
        // reached the client as INTERNAL_ERROR, which reads as a server bug rather than "somebody deleted that role".
        assertThatThrownBy(() -> customRoleService.updateCustomRole(404L, "Name", "Desc", Set.of("WORKFLOW_VIEW")))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("does not exist");

        verify(customRoleRepository, never()).save(any(CustomRole.class));
    }

    @Test
    void testCreateCustomRoleRejectsADuplicateName() {
        when(customRoleRepository.existsByName("Auditor")).thenReturn(true);

        assertThatThrownBy(() -> customRoleService.createCustomRole("Auditor", "Desc", Set.of("WORKFLOW_VIEW")))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already exists");

        // Checked rather than caught: PostgreSQL aborts the transaction on the uk_custom_role_name violation, so a
        // handler that swallowed DuplicateKeyException would still fail at commit.
        verify(customRoleRepository, never()).save(any(CustomRole.class));
    }

    @Test
    void testCreateCustomRoleRejectsTheNameOfABuiltInRole() {
        assertThatThrownBy(() -> customRoleService.createCustomRole("admin", "Desc", Set.of("WORKFLOW_VIEW")))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("built-in role");

        verify(customRoleRepository, never()).save(any(CustomRole.class));
    }

    @Test
    void testUpdateCustomRoleRejectsRenamingToABuiltInRole() {
        when(customRoleRepository.findById(1L))
            .thenReturn(Optional.of(new CustomRole("Old Name", Set.of("WORKFLOW_VIEW"))));

        assertThatThrownBy(() -> customRoleService.updateCustomRole(1L, "Editor", "Desc", Set.of("WORKFLOW_VIEW")))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("built-in role");

        verify(customRoleRepository, never()).save(any(CustomRole.class));
    }

    @Test
    void testUpdateCustomRoleRejectsANameAnotherRoleHolds() {
        when(customRoleRepository.findById(1L))
            .thenReturn(Optional.of(new CustomRole("Old Name", Set.of("WORKFLOW_VIEW"))));
        when(customRoleRepository.existsByName("Auditor")).thenReturn(true);

        assertThatThrownBy(() -> customRoleService.updateCustomRole(1L, "Auditor", "Desc", Set.of("WORKFLOW_VIEW")))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("already exists");

        verify(customRoleRepository, never()).save(any(CustomRole.class));
    }

    @Test
    void testUpdateCustomRoleAllowsARoleToKeepItsOwnName() {
        when(customRoleRepository.findById(1L))
            .thenReturn(Optional.of(new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"))));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // A description-only edit resends the name the role already holds. Its own row satisfies existsByName, so a
        // check that only asked the repository would make every such edit impossible.
        CustomRole result = customRoleService.updateCustomRole(1L, "Auditor", "New desc", Set.of("WORKFLOW_VIEW"));

        assertThat(result.getDescription()).isEqualTo("New desc");

        verify(customRoleRepository, never()).existsByName("Auditor");
    }
}

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
import com.bytechef.ee.automation.configuration.audit.CustomRoleAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.CustomRoleScope;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        customRoleService = new CustomRoleServiceImpl(
            mock(CustomRoleAuditPublisher.class), customRoleRepository, permissionScopeRegistry, permissionService,
            workspaceUserService);
    }

    @Test
    void testCreateCustomRolePersistsCorrectScopes() {
        Set<String> scopeNames = Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT");

        CustomRole savedRole = new CustomRole("Custom Editor", scopeNames);

        savedRole.setDescription("Can view and edit workflows");

        when(customRoleRepository.save(any(CustomRole.class))).thenReturn(savedRole);

        CustomRole result = customRoleService.createCustomRole(
            "Custom Editor", "Can view and edit workflows", scopeNames);

        assertThat(result.getName()).isEqualTo("Custom Editor");
        assertThat(result.getScopes()).hasSize(2);
        assertThat(result.getScopes()).extracting(CustomRoleScope::scope)
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_EDIT");

        verify(customRoleRepository).save(any(CustomRole.class));
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
        when(workspaceUserService.countByCustomRoleId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> customRoleService.deleteCustomRole(1L))
            .isInstanceOf(ConfigurationException.class);

        verify(customRoleRepository, never()).deleteById(1L);
    }

    @Test
    void testDeleteCustomRoleNotInUseSucceeds() {
        when(workspaceUserService.countByCustomRoleId(1L)).thenReturn(0L);

        customRoleService.deleteCustomRole(1L);

        verify(customRoleRepository).deleteById(1L);
    }

    @Test
    void testGetPermissionScopeNamesComesFromTheRegistry() {
        // The same registry the write path validates against — a client editor built from a different list would
        // either offer names the server rejects or omit ones a module added later.
        assertThat(customRoleService.getPermissionScopeNames())
            .containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_CREATE", "WORKFLOW_EDIT");
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
}

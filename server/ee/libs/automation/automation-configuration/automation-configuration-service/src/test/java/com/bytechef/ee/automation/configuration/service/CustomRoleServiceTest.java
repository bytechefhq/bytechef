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
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.exception.ConfigurationException;
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
    private PermissionService permissionService;
    private ProjectUserService projectUserService;
    private CustomRoleServiceImpl customRoleService;

    @BeforeEach
    void setUp() {
        customRoleRepository = mock(CustomRoleRepository.class);
        permissionService = mock(PermissionService.class);
        projectUserService = mock(ProjectUserService.class);

        customRoleService = new CustomRoleServiceImpl(
            customRoleRepository, permissionService, projectUserService);
    }

    @Test
    void testCreateCustomRolePersistsCorrectScopes() {
        Set<PermissionScope> scopes = Set.of(PermissionScope.WORKFLOW_VIEW, PermissionScope.WORKFLOW_EDIT);

        CustomRole savedRole = new CustomRole("Custom Editor", scopes);

        savedRole.setDescription("Can view and edit workflows");

        when(customRoleRepository.save(any(CustomRole.class))).thenReturn(savedRole);

        CustomRole result = customRoleService.createCustomRole("Custom Editor", "Can view and edit workflows", scopes);

        assertThat(result.getName()).isEqualTo("Custom Editor");
        assertThat(result.getScopes()).hasSize(2);
        assertThat(result.getScopes()).extracting(CustomRoleScope::scope)
            .containsExactlyInAnyOrder(PermissionScope.WORKFLOW_VIEW, PermissionScope.WORKFLOW_EDIT);

        verify(customRoleRepository).save(any(CustomRole.class));
    }

    @Test
    void testDeleteCustomRoleInUseThrowsException() {
        when(projectUserService.countByCustomRoleId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> customRoleService.deleteCustomRole(1L))
            .isInstanceOf(ConfigurationException.class);

        verify(customRoleRepository, never()).deleteById(1L);
    }

    @Test
    void testDeleteCustomRoleNotInUseSucceeds() {
        when(projectUserService.countByCustomRoleId(1L)).thenReturn(0L);

        customRoleService.deleteCustomRole(1L);

        verify(customRoleRepository).deleteById(1L);
    }

    @Test
    void testUpdateCustomRoleReplacesScopes() {
        CustomRole existingRole = new CustomRole("Old Name", Set.of(PermissionScope.WORKFLOW_VIEW));

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Set<PermissionScope> newScopes = Set.of(
            PermissionScope.WORKFLOW_VIEW, PermissionScope.WORKFLOW_CREATE, PermissionScope.WORKFLOW_EDIT);

        CustomRole result = customRoleService.updateCustomRole(1L, "New Name", "Updated description", newScopes);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getScopes()).hasSize(3);

        verify(permissionService).evictAllProjectScopeCache();
    }

    @Test
    void testUpdateCustomRoleEvictsCache() {
        CustomRole existingRole = new CustomRole("Role", Set.of(PermissionScope.WORKFLOW_EDIT));

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customRoleService.updateCustomRole(1L, "Role", "Desc", Set.of(PermissionScope.WORKFLOW_VIEW));

        verify(permissionService).evictAllProjectScopeCache();
    }

    @Test
    void testUpdateCustomRoleSkipsCacheEvictionWhenScopesUnchanged() {
        // Name-only / description-only edits must NOT evict the global project scope cache. For tenants with
        // thousands of projects the eviction cost is meaningful, and no user's resolved permissions actually change.
        // If this assertion regresses, someone changed the scope-equality check in updateCustomRole and re-introduced
        // the perf cliff.
        Set<PermissionScope> scopes = Set.of(PermissionScope.WORKFLOW_VIEW, PermissionScope.WORKFLOW_EDIT);
        CustomRole existingRole = new CustomRole("Old Name", scopes);

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(existingRole));
        when(customRoleRepository.save(any(CustomRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customRoleService.updateCustomRole(1L, "New Name", "New description", scopes);

        verify(permissionService, never()).evictAllProjectScopeCache();
    }
}

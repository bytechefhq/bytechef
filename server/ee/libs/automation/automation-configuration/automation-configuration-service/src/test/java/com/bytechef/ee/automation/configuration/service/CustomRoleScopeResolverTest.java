/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomRoleScopeResolverTest {

    private CustomRoleRepository customRoleRepository;
    private CustomRoleScopeResolverImpl customRoleScopeResolver;

    @BeforeEach
    void setUp() {
        customRoleRepository = mock(CustomRoleRepository.class);

        PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);

        when(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.AUTOMATION))
            .thenReturn(Set.of("CONNECTION_VIEW", "WORKFLOW_VIEW"));
        when(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED))
            .thenReturn(Set.of("INTEGRATION_CREATE", "WORKFLOW_VIEW"));

        customRoleScopeResolver = new CustomRoleScopeResolverImpl(customRoleRepository, permissionScopeRegistry);
    }

    @Test
    void testResolveScopesReturnsCorrectScopes() {
        CustomRole customRole = new CustomRole("Test Role", Set.of("WORKFLOW_VIEW", "CONNECTION_VIEW"));

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(customRole));

        Optional<Set<String>> scopes = customRoleScopeResolver.resolveScopes(1L);

        assertThat(scopes)
            .isPresent()
            .hasValueSatisfying(
                resolvedScopes -> assertThat(resolvedScopes)
                    .containsExactlyInAnyOrder("WORKFLOW_VIEW", "CONNECTION_VIEW"));
    }

    @Test
    void testResolveScopesReturnsTheEmbeddedScopesOfAnEmbeddedRole() {
        CustomRole customRole = new CustomRole(
            "Integrator", PermissionScopeSide.EMBEDDED, Set.of("INTEGRATION_CREATE", "PROJECT_CREATE"));

        when(customRoleRepository.findById(3L)).thenReturn(Optional.of(customRole));

        assertThat(customRoleScopeResolver.resolveScopes(3L))
            .hasValueSatisfying(resolvedScopes -> assertThat(resolvedScopes).containsExactly("INTEGRATION_CREATE"));
    }

    @Test
    void testResolveScopesDropsScopesNoModuleDeclares() {
        CustomRole customRole = new CustomRole("Stale Role", Set.of("WORKFLOW_VIEW", "RENAMED_SCOPE"));

        when(customRoleRepository.findById(2L)).thenReturn(Optional.of(customRole));

        assertThat(customRoleScopeResolver.resolveScopes(2L))
            .hasValueSatisfying(resolvedScopes -> assertThat(resolvedScopes).containsExactly("WORKFLOW_VIEW"));
    }

    @Test
    void testResolveScopesReturnsEmptyOptionalForMissingRole() {
        when(customRoleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Set<String>> scopes = customRoleScopeResolver.resolveScopes(999L);

        // Missing role returns Optional.empty() (orphan ref) so callers can distinguish from "role exists with no
        // scopes", which the CustomRole notEmpty invariant forbids at the persistence layer.
        assertThat(scopes).isEmpty();
    }
}

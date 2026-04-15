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
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
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

        customRoleScopeResolver = new CustomRoleScopeResolverImpl(customRoleRepository);
    }

    @Test
    void testResolveScopesReturnsCorrectScopes() {
        CustomRole customRole = new CustomRole(
            "Test Role", Set.of(PermissionScope.WORKFLOW_VIEW, PermissionScope.EXECUTION_VIEW));

        when(customRoleRepository.findById(1L)).thenReturn(Optional.of(customRole));

        Optional<Set<PermissionScope>> scopes = customRoleScopeResolver.resolveScopes(1L);

        assertThat(scopes)
            .isPresent()
            .hasValueSatisfying(s -> assertThat(s).containsExactlyInAnyOrder(
                PermissionScope.WORKFLOW_VIEW, PermissionScope.EXECUTION_VIEW));
    }

    @Test
    void testResolveScopesReturnsEmptyOptionalForMissingRole() {
        when(customRoleRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Set<PermissionScope>> scopes = customRoleScopeResolver.resolveScopes(999L);

        // Missing role returns Optional.empty() (orphan ref) so callers can distinguish from "role exists with no
        // scopes", which the CustomRole notEmpty invariant forbids at the persistence layer.
        assertThat(scopes).isEmpty();
    }
}

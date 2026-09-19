/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.exception.CustomRoleErrorType;
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.exception.ConfigurationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * The controller carries two pieces of behaviour worth testing on their own: the request-shape validation in
 * {@code toScopeNames}, and the {@code workspaceId} pass-through.
 * <p>
 * Both matter for authorization. A role with zero scopes grants nothing and silently strips access from everyone
 * holding it, which is why absent and empty are refused identically and as a typed {@code SCOPES_REQUIRED} rather than
 * as a bare {@code IllegalArgumentException} that would reach the client as {@code INTERNAL_ERROR}. And the nullness of
 * {@code workspaceId} is what selects the authorization tier on {@code CustomRoleServiceImpl.getCustomRoles} — null
 * means tenant-admin-only, non-null means the {@code 'Workspace'} scope check — so a controller that defaulted the
 * argument or dropped it would move every caller onto the wrong tier.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomRoleGraphQlControllerTest {

    private final CustomRoleService customRoleService = mock(CustomRoleService.class);
    private final CustomRoleGraphQlController controller = new CustomRoleGraphQlController(customRoleService);

    @Test
    void testCreateCustomRoleRejectsAnAbsentScopesField() {
        assertScopesRequired(() -> controller.createCustomRole(inputWithoutScopes()));

        verify(customRoleService, never()).createCustomRole(anyString(), anyString(), any());
    }

    @Test
    void testCreateCustomRoleRejectsAnEmptyScopesList() {
        assertScopesRequired(() -> controller.createCustomRole(input(List.of())));

        verify(customRoleService, never()).createCustomRole(anyString(), anyString(), any());
    }

    @Test
    void testUpdateCustomRoleRejectsAnAbsentScopesField() {
        assertScopesRequired(() -> controller.updateCustomRole(1L, inputWithoutScopes()));

        verify(customRoleService, never()).updateCustomRole(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void testUpdateCustomRoleRejectsAnEmptyScopesList() {
        assertScopesRequired(() -> controller.updateCustomRole(1L, input(List.of())));

        verify(customRoleService, never()).updateCustomRole(anyLong(), anyString(), anyString(), any());
    }

    @Test
    void testCreateCustomRolePassesTheRequestedScopesToTheService() {
        controller.createCustomRole(input(List.of("WORKFLOW_VIEW", "WORKFLOW_EDIT")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> scopeNamesArgumentCaptor = ArgumentCaptor.forClass(Set.class);

        verify(customRoleService).createCustomRole(
            eq("Auditor"), eq("Reads workflows"), scopeNamesArgumentCaptor.capture());

        // Membership validation is the service's job against the PermissionScopeRegistry; the controller must hand over
        // the names unchanged rather than filtering to a list it thinks it knows.
        assertThat(scopeNamesArgumentCaptor.getValue()).containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_EDIT");
    }

    /**
     * A null {@code workspaceId} must reach the service as null, because that is the value its {@code @PreAuthorize}
     * branches on: {@code (#workspaceId == null and isTenantAdmin())} against
     * {@code (#workspaceId != null and hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE'))}. A
     * controller that substituted a default — a current workspace, or zero — would silently move a tenant-admin read
     * onto the workspace tier and refuse it.
     */
    @Test
    void testCustomRolesPassesANullWorkspaceIdThroughUntouched() {
        when(customRoleService.getCustomRoles(null)).thenReturn(List.of());

        assertThat(controller.customRoles(null)).isEmpty();

        verify(customRoleService).getCustomRoles(null);
    }

    @Test
    void testCustomRolesPassesAWorkspaceIdThroughUntouched() {
        CustomRole customRole = new CustomRole("Auditor", Set.of("WORKFLOW_VIEW"));

        when(customRoleService.getCustomRoles(7L)).thenReturn(List.of(customRole));

        assertThat(controller.customRoles(7L)).containsExactly(customRole);

        verify(customRoleService).getCustomRoles(7L);
    }

    @Test
    void testScopesUnwrapsTheChildCollectionRowsToNames() {
        CustomRole customRole = new CustomRole("Auditor", Set.of("WORKFLOW_VIEW", "WORKFLOW_EDIT"));

        // Property resolution would otherwise pick getScopes() and serialise the Spring Data JDBC row wrappers as
        // "CustomRoleScope[scope=WORKFLOW_VIEW]", which the schema's [String!]! does not describe.
        assertThat(controller.scopes(customRole)).containsExactlyInAnyOrder("WORKFLOW_VIEW", "WORKFLOW_EDIT");
    }

    private void assertScopesRequired(Runnable invocation) {
        assertThatThrownBy(invocation::run)
            .isInstanceOf(ConfigurationException.class)
            .satisfies(
                exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    // A typed error key, not a bare IllegalArgumentException: the global resolver maps this to
                    // BAD_REQUEST with something the client can switch on.
                    .isEqualTo(CustomRoleErrorType.SCOPES_REQUIRED.getErrorKey()));
    }

    private static Map<String, Object> input(List<String> scopeNames) {
        Map<String, Object> input = inputWithoutScopes();

        input.put("scopes", scopeNames);

        return input;
    }

    private static Map<String, Object> inputWithoutScopes() {
        // A HashMap rather than Map.of, because an absent 'scopes' is what the first case needs and Map.of forbids the
        // null value that would otherwise stand in for it.
        Map<String, Object> input = new HashMap<>();

        input.put("name", "Auditor");
        input.put("description", "Reads workflows");

        return input;
    }
}

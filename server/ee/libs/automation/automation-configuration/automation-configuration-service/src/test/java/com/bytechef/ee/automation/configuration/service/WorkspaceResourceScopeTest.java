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

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.WorkspaceOwnershipResolver;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The {@code 'Workspace'} token is named by the members read, the workspace update, the project listing and the
 * custom-role assignment picker, and it is only a check because {@code WorkspaceOwnershipResolver} is registered for
 * it. {@code hasResourceScope} fails closed on a missed registry lookup, so an unregistered token denies every
 * non-tenant-admin rather than checking anything — and no guard test would notice, because those mock
 * {@code PermissionService} itself while a tenant admin short-circuits ahead of the lookup.
 * <p>
 * Asserted in both directions plus the no-resolver case: a deny-only assertion cannot tell "refuses a non-member" from
 * "refuses everybody", which is the exact failure a deleted resolver produces.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceResourceScopeTest {

    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 42L;

    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

    @Test
    void testWorkspaceScopeIsGrantedToAMemberHoldingIt() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKSPACE_VIEW"));

        assertThat(service().hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).isTrue();
    }

    @Test
    void testWorkspaceScopeIsRefusedForANonMember() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID)).thenReturn(Set.of());

        assertThat(service().hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).isFalse();
    }

    @Test
    void testWorkspaceScopeIsRefusedForAScopeTheMemberDoesNotHold() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKSPACE_VIEW"));

        // The scope is compared, not merely the membership: a viewer must not pass a management gate.
        assertThat(service().hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE")).isFalse();
    }

    /**
     * The identity resolver reads as deletable boilerplate — the id already <em>is</em> the workspace. Deleting it
     * would break no guard test, because those mock {@code PermissionService}. It would silently turn every workspace
     * gate into a lockout for everybody but tenant admins: this is the assertion that fails instead.
     */
    @Test
    void testWorkspaceScopeIsRefusedForEveryoneWithoutTheOwnershipResolver() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKSPACE_VIEW"));

        PermissionServiceImpl serviceWithoutResolver = service(List.of());

        assertThat(serviceWithoutResolver.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_VIEW")).isFalse();
    }

    private PermissionServiceImpl service() {
        return service(ownershipResolvers());
    }

    private PermissionServiceImpl service(List<ResourceOwnershipResolver> resourceOwnershipResolvers) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, resourceOwnershipResolvers, List.of());
    }

    private static List<ResourceOwnershipResolver> ownershipResolvers() {
        return List.of(new WorkspaceOwnershipResolver());
    }

}

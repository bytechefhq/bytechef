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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ProjectOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * {@code 'Project'} is the most-used resource token in the tree — every project read, export, publish and delete names
 * it — and it is only a check because {@code ProjectOwnershipResolver} is registered for it. Nothing else in the suite
 * covers that: the guard tests mock {@code PermissionService}, and a tenant admin's short-circuit runs before the
 * registry is consulted, so manual testing as an admin cannot see the difference either.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectResourceScopeTest {

    private static final long PROJECT_ID = 3L;
    private static final long USER_ID = 7L;
    private static final long WORKSPACE_ID = 42L;

    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final PermissionScopeRegistry permissionScopeRegistry = mock(PermissionScopeRegistry.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = mock(WorkspaceUserRepository.class);

    @Test
    void testProjectScopeIsGrantedFromTheRoleHeldInTheOwningWorkspace() {
        givenProjectOwnedByWorkspace();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        assertThat(service().hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isTrue();
    }

    @Test
    void testProjectScopeIsRefusedForANonMemberOfTheOwningWorkspace() {
        givenProjectOwnedByWorkspace();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID)).thenReturn(Set.of());

        assertThat(service().hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testProjectScopeIsRefusedForAnUnknownProject() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        // Fails closed on the resolver's unknown(): a deleted project must not inherit the caller's scopes in some
        // other workspace.
        assertThat(service().hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isFalse();
    }

    /**
     * Deleting {@code ProjectOwnershipResolver} — or mistyping its {@code resourceType()} — would break no guard test.
     * It would silently turn every {@code 'Project'} gate in the tree into a lockout for every non-tenant-admin: this
     * is the assertion that fails instead.
     */
    @Test
    void testProjectScopeIsRefusedForEveryoneWithoutTheOwnershipResolver() {
        givenProjectOwnedByWorkspace();

        when(workspaceScopeCacheService.getWorkspaceScopes(USER_ID, WORKSPACE_ID))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        PermissionServiceImpl serviceWithoutResolver = service(List.of());

        assertThat(serviceWithoutResolver.hasResourceScope(PROJECT_ID, "Project", "WORKFLOW_VIEW")).isFalse();
    }

    private void givenProjectOwnedByWorkspace() {
        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(USER_ID));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    private PermissionServiceImpl service() {
        return service(ownershipResolvers());
    }

    private PermissionServiceImpl service(List<ResourceOwnershipResolver> resourceOwnershipResolvers) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, resourceOwnershipResolvers, List.of());
    }

    private List<ResourceOwnershipResolver> ownershipResolvers() {
        return List.of(new ProjectOwnershipResolver(projectRepository));
    }

}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Pins the EE behavior of {@code hasResourceScope} (workspace-scope path, fail-closed when no workspace) and
 * {@code isResourceOwner} (isCurrentUser path).
 *
 * @author Ivica Cardic
 * @version ee
 */
class PermissionServiceResourceTest {

    private final CurrentUserResolver currentUserResolver = Mockito.mock(CurrentUserResolver.class);
    private final PermissionScopeRegistry permissionScopeRegistry = Mockito.mock(PermissionScopeRegistry.class);
    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final WorkspaceScopeCacheService workspaceScopeCacheService =
        Mockito.mock(WorkspaceScopeCacheService.class);
    private final WorkspaceUserRepository workspaceUserRepository = Mockito.mock(WorkspaceUserRepository.class);

    private PermissionServiceImpl service(ResourceOwnershipResolver... resolvers) {
        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, List.of(resolvers));
    }

    private static ResourceOwnershipResolver resolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }
        };
    }

    @Test
    void testHasResourceScopeUsesWorkspaceScopeInEe() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L))
            .thenReturn(Set.of("CONNECTION_DELETE"));

        PermissionServiceImpl service = service(resolver("Connection", ResourceOwner.ofWorkspace(42L)));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeNoWorkspaceFailsClosed() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("Connection", ResourceOwner.unknown()));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testIsResourceOwnerMatchAllowsInEe() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("ApiKey", ResourceOwner.ofUser(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testIsResourceOwnerMismatchDeniesInEe() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(99L));

        PermissionServiceImpl service = service(resolver("ApiKey", ResourceOwner.ofUser(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isFalse();
    }

    @Test
    void testHasResourceRoleChecksWorkspaceRoleInEe() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        com.bytechef.ee.automation.configuration.domain.WorkspaceUser workspaceUser =
            Mockito.mock(com.bytechef.ee.automation.configuration.domain.WorkspaceUser.class);

        when(workspaceUser.getWorkspaceRole())
            .thenReturn(com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole.EDITOR.ordinal());
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(7L, 42L))
            .thenReturn(java.util.Optional.of(workspaceUser));

        PermissionServiceImpl service = service(resolver("KnowledgeBase", ResourceOwner.ofWorkspace(42L)));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isTrue();
    }

    @Test
    void testHasResourceRoleNoWorkspaceFailsClosedInEe() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("KnowledgeBase", ResourceOwner.unknown()));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testHasWorkflowScopeResolvesProjectWorkspaceInEe() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        com.bytechef.automation.configuration.domain.Project project =
            Mockito.mock(com.bytechef.automation.configuration.domain.Project.class);

        when(project.getWorkspaceId()).thenReturn(42L);
        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(java.util.Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        PermissionServiceImpl service = service();

        assertThat(service.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkflowScopeUnknownWorkflowFailsClosedInEe() {
        when(projectRepository.findByWorkflowId("nope")).thenReturn(java.util.Optional.empty());

        PermissionServiceImpl service = service();

        assertThat(service.hasWorkflowScope("nope", "WORKFLOW_EDIT")).isFalse();
    }
}

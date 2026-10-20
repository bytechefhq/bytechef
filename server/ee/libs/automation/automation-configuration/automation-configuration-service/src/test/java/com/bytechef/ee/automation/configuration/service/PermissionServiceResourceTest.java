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
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

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

    private final EmbeddedScopeCacheService embeddedScopeCacheService = Mockito.mock(EmbeddedScopeCacheService.class);

    private PermissionServiceImpl service(ResourceOwnershipResolver... resolvers) {
        return service(List.of(resolvers), List.of());
    }

    private PermissionServiceImpl service(
        List<ResourceOwnershipResolver> ownershipResolvers, List<ResourceEnvironmentResolver> environmentResolvers) {

        return new PermissionServiceImpl(
            currentUserResolver, permissionScopeRegistry, projectRepository, workspaceScopeCacheService,
            workspaceUserRepository, ownershipResolvers, environmentResolvers,
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            embeddedScopeCacheService, Mockito.mock(EmbeddedUserRepository.class));
    }

    private static ResourceEnvironmentResolver environmentResolver(String type, Optional<Environment> environment) {
        return new ResourceEnvironmentResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                return environment;
            }
        };
    }

    private static ResourceEnvironmentResolver throwingEnvironmentResolver(String type) {
        return new ResourceEnvironmentResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public Optional<Environment> fetchEnvironment(Serializable id) {
                throw new IllegalStateException("boom");
            }
        };
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
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(7L, 42L))
            .thenReturn(Optional.of(workspaceUser));

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
        when(projectRepository.findByWorkflowId("wf-uuid")).thenReturn(Optional.of(project));
        when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L))
            .thenReturn(Set.of("WORKFLOW_EDIT"));

        PermissionServiceImpl service = service();

        assertThat(service.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkflowScopeUnknownWorkflowFailsClosedInEe() {
        when(projectRepository.findByWorkflowId("nope")).thenReturn(Optional.empty());

        PermissionServiceImpl service = service();

        assertThat(service.hasWorkflowScope("nope", "WORKFLOW_EDIT")).isFalse();
    }

    @Test
    void testTwoResolversForOneTypeChainUntilOneKnowsTheOwner() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L)).thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.unknown()),
                resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of());

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isTrue();
    }

    @Test
    void testAnEmbeddedOwnerWithAnEnvironmentIsCheckedInThatEnvironment() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.DEVELOPMENT))
            .thenReturn(Set.of("EMBEDDED_VIEW"));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.PRODUCTION)).thenReturn(Set.of());

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.of(Environment.PRODUCTION))));

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isFalse();
    }

    @Test
    void testAnEmbeddedOwnerWithoutAnEnvironmentUsesTheUnawareAnswer() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L)).thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.empty())));

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isTrue();
    }

    @Test
    void testEveryResolverUnknownDeniesWithoutThrowing() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.unknown()), resolver("Connection", ResourceOwner.unknown())),
            List.of());

        assertThat(service.hasResourceScope(1L, "Connection", "EMBEDDED_VIEW")).isFalse();
    }

    @Test
    void testAnEnvironmentResolverThatThrowsDenies() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));
        lenient().when(workspaceScopeCacheService.getWorkspaceScopes(7L, 42L))
            .thenReturn(Set.of("CONNECTION_DELETE"));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.unknown()),
                resolver("Connection", ResourceOwner.ofWorkspace(42L))),
            List.of(
                environmentResolver("Connection", Optional.empty()), throwingEnvironmentResolver("Connection")));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testAnEmbeddedOwnerIsCheckedInTheNamedEnvironment() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));
        when(embeddedScopeCacheService.getEmbeddedScopes(7L, Environment.STAGING)).thenReturn(Set.of("EMBEDDED_VIEW"));

        PermissionServiceImpl service = service(resolver("Connection", ResourceOwner.ofEmbedded()));

        assertThat(service.hasResourceScopeInEnvironment(1L, "Connection", "EMBEDDED_VIEW", Environment.STAGING))
            .isTrue();
    }

    @Test
    void testIsResourceOwnerComparesTheOwnerOfAnEmbeddedResource() {
        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("ApiKey", ResourceOwner.ofEmbedded(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testAnEmbeddedOwnerIsDeniedByResourceRole() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(resolver("KnowledgeBase", ResourceOwner.ofEmbedded()));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "VIEWER")).isFalse();
    }

    @Test
    void testAnEmbeddedConnectionIsDeniedInAWorkspace() {
        lenient().when(currentUserResolver.fetchCurrentUserId())
            .thenReturn(OptionalLong.of(7L));

        PermissionServiceImpl service = service(
            List.of(resolver("Connection", ResourceOwner.ofEmbedded())),
            List.of(environmentResolver("Connection", Optional.of(Environment.PRODUCTION))));

        assertThat(service.canUseConnectionInWorkspace(1L, 42L, Environment.PRODUCTION)).isFalse();
    }
}

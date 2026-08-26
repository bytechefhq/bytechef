/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins that visibility is a precondition of every resource-scope check rather than a filter running beside it.
 *
 * <p>
 * The regression this guards: a workspace member holding {@code CONNECTION_EDIT} used to pass {@code hasResourceScope}
 * for <em>any</em> connection in their workspace, including one a colleague had made PRIVATE. The connections list hid
 * it; the by-id path did not. If this class is ever deleted or weakened, the list and by-id halves of the same
 * authorization question can silently disagree again.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionServiceVisibilityTest {

    private static final String CONNECTION = "Connection";
    private static final String PROJECT = "Project";
    private static final String WORKFLOW = "Workflow";
    private static final String PRIVATE_WORKFLOW_ID = "wf-private";
    private static final long PRIVATE_CONNECTION_ID = 10L;
    private static final long PRIVATE_PROJECT_ID = 20L;
    private static final long PRIVATE_PROJECT_FOR_SCOPE_ID = 30L;
    private static final long WORKSPACE_CONNECTION_ID = 11L;
    private static final long WORKSPACE_ID = 1L;

    private static final AtomicReference<String> LAST_RESOLVED_TYPE = new AtomicReference<>();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    login, "password", List.of(authorities)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()));
    }

    @Test
    void testPrivateResourceDeniedToNonOwnerHoldingTheScope() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
            .as("ana holds CONNECTION_EDIT in the workspace, but the connection is ivica's and PRIVATE")
            .isFalse();
    }

    @Test
    void testPrivateResourceAllowedToGrantee() {
        authenticate("ana");

        assertThat(
            permissionService(Set.of(PRIVATE_CONNECTION_ID))
                .hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
                    .as("a grant restores the access ana would have had if the connection were workspace-visible")
                    .isTrue();
    }

    @Test
    void testWorkspaceResourceAllowedToMemberHoldingTheScope() {
        authenticate("ana");

        assertThat(
            permissionService(Set.of()).hasResourceScope(WORKSPACE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
                .isTrue();
    }

    @Test
    void testPrivateResourceAllowedToOwner() {
        authenticate("ivica");

        assertThat(permissionService(Set.of()).hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
            .isTrue();
    }

    @Test
    void testTenantAdminBypassesVisibility() {
        authenticate("marko", AuthorityConstants.ADMIN);

        assertThat(permissionService(Set.of()).hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
            .isTrue();
    }

    @Test
    void testUnknownResourceFailsClosed() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope(999L, CONNECTION, "CONNECTION_EDIT"))
            .as("a registered resource type whose row does not exist must deny, not fall through to allow")
            .isFalse();
    }

    @Test
    void testResourceTypeWithoutProviderIsUnrestrictedByVisibility() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope(5L, "ApiKey", "API_KEY_EDIT"))
            .as("a resource family that has not opted into visibility keeps its previous behaviour")
            .isTrue();
    }

    @Test
    void testInheritingResourceIsGrantedThroughItsParent() {
        authenticate("ana");

        assertThat(
            permissionServiceWithWorkflow(Set.of(PRIVATE_PROJECT_ID))
                .hasResourceScope(PRIVATE_WORKFLOW_ID, WORKFLOW, "WORKFLOW_VIEW"))
                    .as("the grant sits on (Project, 20); the workflow provider must resolve against it")
                    .isTrue();
    }

    @Test
    void testInheritingResourceIsDeniedWithoutParentGrant() {
        authenticate("ana");

        assertThat(
            permissionServiceWithWorkflow(Set.of()).hasResourceScope(PRIVATE_WORKFLOW_ID, WORKFLOW, "WORKFLOW_VIEW"))
                .isFalse();
    }

    @Test
    void testInheritingResourceResolvesUnderParentType() {
        authenticate("ana");

        permissionServiceWithWorkflow(Set.of(PRIVATE_PROJECT_ID))
            .hasResourceScope(PRIVATE_WORKFLOW_ID, WORKFLOW, "WORKFLOW_VIEW");

        assertThat(LAST_RESOLVED_TYPE.get())
            .as("grants for a workflow live under its project, so the resolver must be asked about Project")
            .isEqualTo(PROJECT);
    }

    @Test
    void testHasWorkflowScopeDeniesWorkflowInsideHiddenProject() {
        authenticate("ana");

        assertThat(permissionServiceWithWorkflow(Set.of()).hasWorkflowScope(PRIVATE_WORKFLOW_ID, "WORKFLOW_VIEW"))
            .as("the workflow-keyed entry point must not bypass visibility")
            .isFalse();
    }

    @Test
    void testHasWorkflowScopeAllowsWorkflowInsideGrantedProject() {
        authenticate("ana");

        assertThat(
            permissionServiceWithWorkflow(Set.of(PRIVATE_PROJECT_ID))
                .hasWorkflowScope(PRIVATE_WORKFLOW_ID, "WORKFLOW_VIEW"))
                    .as("ana genuinely holds WORKFLOW_VIEW in the workspace, so a grant is all that was missing")
                    .isTrue();
    }

    @Test
    void testStringIdOnNumericOnlyProviderFailsClosed() {
        authenticate("ana");

        assertThat(permissionService(Set.of()).hasResourceScope("not-a-number", CONNECTION, "CONNECTION_EDIT"))
            .as("the default Serializable overload rejects non-numeric ids for a numeric-keyed provider")
            .isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectDeniesHiddenProject() {
        authenticate("ana");

        PermissionServiceImpl permissionService = permissionServiceWithProject(Set.of());

        assertThat(permissionService.hasWorkspaceScopeForProject(PRIVATE_PROJECT_FOR_SCOPE_ID, "WORKFLOW_VIEW"))
            .as("the project-keyed entry point must not bypass visibility")
            .isFalse();
        assertThat(permissionServiceWithProject(Set.of(PRIVATE_PROJECT_FOR_SCOPE_ID))
            .hasWorkspaceScopeForProject(PRIVATE_PROJECT_FOR_SCOPE_ID, "WORKFLOW_VIEW"))
                .as("ana genuinely holds WORKFLOW_VIEW in the workspace, so a grant is all that was missing")
                .isTrue();
    }

    @Test
    void testHasWorkspaceScopeForProjectWithEnvironmentDeniesHiddenProject() {
        authenticate("ana");

        PermissionServiceImpl permissionService = permissionServiceWithProject(Set.of());

        assertThat(
            permissionService.hasWorkspaceScopeForProject(
                PRIVATE_PROJECT_FOR_SCOPE_ID, "WORKFLOW_VIEW", Environment.PRODUCTION))
                    .as("the promotion-aware overload must not bypass visibility either")
                    .isFalse();
        assertThat(
            permissionServiceWithProject(Set.of(PRIVATE_PROJECT_FOR_SCOPE_ID))
                .hasWorkspaceScopeForProject(PRIVATE_PROJECT_FOR_SCOPE_ID, "WORKFLOW_VIEW", Environment.PRODUCTION))
                    .as("ana genuinely holds WORKFLOW_VIEW in PRODUCTION, so a grant is all that was missing")
                    .isTrue();
    }

    /**
     * Builds the EE service with a stubbed workspace-scope check that always grants, so any denial in these tests can
     * only come from the visibility precondition.
     */
    private static PermissionServiceImpl permissionService(Set<Long> grantedConnectionIds) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong()))
            .thenReturn(Set.of("CONNECTION_EDIT", "API_KEY_EDIT"));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), mock(ProjectRepository.class),
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(connectionOwnershipResolver(), apiKeyOwnershipResolver()),
            List.of(connectionVisibilityProvider()), visibilityResolver(grantedConnectionIds), List.of(),
            mock(ObjectProvider.class));
    }

    private static ResourceOwnershipResolver connectionOwnershipResolver() {
        return ownershipResolver(CONNECTION);
    }

    private static ResourceOwnershipResolver apiKeyOwnershipResolver() {
        return ownershipResolver("ApiKey");
    }

    private static ResourceOwnershipResolver ownershipResolver(String resourceType) {
        return new ResourceOwnershipResolver() {

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return ResourceOwner.ofWorkspace(WORKSPACE_ID);
            }

            @Override
            public ResourceOwner resolveOwner(Serializable id) {
                return ResourceOwner.ofWorkspace(WORKSPACE_ID);
            }
        };
    }

    private static ResourceVisibilityProvider connectionVisibilityProvider() {
        return new ResourceVisibilityProvider() {

            @Override
            public String resourceType() {
                return CONNECTION;
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                if (id == PRIVATE_CONNECTION_ID) {
                    return Optional.of(new VisibilityRecord(id, ResourceVisibility.PRIVATE, "ivica"));
                }

                if (id == WORKSPACE_CONNECTION_ID) {
                    return Optional.of(new VisibilityRecord(id, ResourceVisibility.WORKSPACE, "ivica"));
                }

                return Optional.empty();
            }
        };
    }

    /**
     * The real resolution order, minus the grant table: admin, reach, ownership, then the supplied grant set.
     */
    private static ResourceVisibilityResolver visibilityResolver(Set<Long> grantedIds) {
        return (resourceType, workspaceId, candidates) -> {
            Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

            if (authentication == null) {
                return Set.of();
            }

            boolean admin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> AuthorityConstants.ADMIN.equals(authority.getAuthority()));
            String login = String.valueOf(authentication.getPrincipal());

            return candidates.stream()
                .filter(
                    candidate -> admin ||
                        candidate.visibility()
                            .isAtLeast(ResourceVisibility.WORKSPACE)
                        || login.equals(candidate.createdBy()) || grantedIds.contains(candidate.id()))
                .map(VisibilityRecord::id)
                .collect(Collectors.toSet());
        };
    }

    /**
     * Builds the EE service with a single workflow provider, so the only thing under test is that a resource which
     * inherits its reach resolves against its parent's type and id.
     *
     * <p>
     * The repository is stubbed to resolve the workflow to a workspace even though {@code hasResourceScope} never
     * consults it: {@code hasWorkflowScope} used to, and that stub is what makes the denial tests genuinely red against
     * the old body. Without it they would pass because the workflow resolved to no workspace, not because visibility
     * denied.
     */
    private static PermissionServiceImpl permissionServiceWithWorkflow(Set<Long> grantedProjectIds) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong())).thenReturn(Set.of("WORKFLOW_VIEW"));

        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findByWorkflowId(PRIVATE_WORKFLOW_ID)).thenReturn(Optional.of(project));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class), List.of(ownershipResolver(WORKFLOW)),
            List.of(workflowVisibilityProvider()), capturingResolver(grantedProjectIds), List.of(),
            mock(ObjectProvider.class));
    }

    /**
     * A workflow inherits its project's reach: the record it returns is the PROJECT's (id, visibility, owner) and it
     * declares "Project" as the type its grants live under.
     */
    private static ResourceVisibilityProvider workflowVisibilityProvider() {
        return new ResourceVisibilityProvider() {

            @Override
            public String resourceType() {
                return WORKFLOW;
            }

            @Override
            public String visibilityResourceType() {
                return PROJECT;
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                return Optional.empty();
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(Serializable id) {
                if (PRIVATE_WORKFLOW_ID.equals(id)) {
                    return Optional.of(new VisibilityRecord(PRIVATE_PROJECT_ID, ResourceVisibility.PRIVATE, "ivica"));
                }

                return Optional.empty();
            }
        };
    }

    /**
     * Records the resource type the service hands the resolver, so a test can pin that an inheriting resource is
     * resolved under its parent's type rather than its own.
     */
    private static ResourceVisibilityResolver capturingResolver(Set<Long> grantedIds) {
        ResourceVisibilityResolver delegate = visibilityResolver(grantedIds);

        return (resourceType, workspaceId, candidates) -> {
            LAST_RESOLVED_TYPE.set(resourceType);

            return delegate.filterVisibleIds(resourceType, workspaceId, candidates);
        };
    }

    /**
     * Builds the EE service with a single project provider and workspace-scope stubs that grant {@code WORKFLOW_VIEW}
     * both environment-unaware and per-environment, so the only thing that can deny a project-keyed check — through
     * either overload — is the visibility precondition. The repository is stubbed as well because the environment-aware
     * overload resolves the owning workspace through it rather than through the ownership resolver.
     */
    private static PermissionServiceImpl permissionServiceWithProject(Set<Long> grantedProjectIds) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong())).thenReturn(Set.of("WORKFLOW_VIEW"));
        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong(), any(Environment.class)))
            .thenReturn(Set.of("WORKFLOW_VIEW"));

        Project project = new Project();

        project.setWorkspaceId(WORKSPACE_ID);

        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findById(PRIVATE_PROJECT_FOR_SCOPE_ID)).thenReturn(Optional.of(project));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class), List.of(ownershipResolver(PROJECT)),
            List.of(projectVisibilityProvider()), visibilityResolver(grantedProjectIds), List.of(),
            mock(ObjectProvider.class));
    }

    private static ResourceVisibilityProvider projectVisibilityProvider() {
        return new ResourceVisibilityProvider() {

            @Override
            public String resourceType() {
                return PROJECT;
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                if (id == PRIVATE_PROJECT_FOR_SCOPE_ID) {
                    return Optional.of(new VisibilityRecord(id, ResourceVisibility.PRIVATE, "ivica"));
                }

                return Optional.empty();
            }
        };
    }
}

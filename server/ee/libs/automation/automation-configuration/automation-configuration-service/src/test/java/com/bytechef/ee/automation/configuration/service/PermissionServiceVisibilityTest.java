/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
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
    private static final long PRIVATE_CONNECTION_ID = 10L;
    private static final long WORKSPACE_CONNECTION_ID = 11L;
    private static final long ORGANIZATION_CONNECTION_ID = 12L;
    private static final long WORKSPACE_ID = 7L;

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

        assertThat(
            permissionService(Set.of()).hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
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

        assertThat(
            permissionService(Set.of()).hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
                .isTrue();
    }

    @Test
    void testGrantingAndRevokingTogglesAccessToAPrivateConnection() {
        authenticate("ana");

        Set<Long> grantedConnectionIds = new HashSet<>();

        PermissionServiceImpl permissionService = permissionService(grantedConnectionIds);

        assertThat(permissionService.hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
            .isFalse();

        grantedConnectionIds.add(PRIVATE_CONNECTION_ID);

        assertThat(permissionService.hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
            .isTrue();

        grantedConnectionIds.remove(PRIVATE_CONNECTION_ID);

        assertThat(permissionService.hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
            .isFalse();
    }

    @Test
    void testTenantAdminBypassesVisibility() {
        authenticate("marko", AuthorityConstants.ADMIN);

        assertThat(
            permissionService(Set.of()).hasResourceScope(PRIVATE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
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
    void testOrganizationConnectionIsViewableByAnyMember() {
        authenticate("ana");

        assertThat(
            permissionService(Set.of()).hasResourceScope(ORGANIZATION_CONNECTION_ID, CONNECTION, "CONNECTION_VIEW"))
                .isTrue();
    }

    @Test
    void testOrganizationConnectionIsNotEditableThroughAWorkspaceScope() {
        authenticate("ana");

        assertThat(
            permissionService(Set.of()).hasResourceScope(ORGANIZATION_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
                .isFalse();
    }

    @Test
    void testPrivateConnectionCannotBeAttachedToAProjectWorkflowByANonOwner() {
        authenticate("ana");

        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findByWorkflowId("workflow-1")).thenReturn(Optional.of(new Project()));

        assertThat(
            permissionService(Set.of(), projectRepository)
                .hasResourceScopeIfProjectWorkflow("workflow-1", PRIVATE_CONNECTION_ID, CONNECTION,
                    "CONNECTION_EDIT"))
                        .isFalse();
    }

    @Test
    void testWorkspaceConnectionCanBeAttachedToAProjectWorkflow() {
        authenticate("ana");

        ProjectRepository projectRepository = mock(ProjectRepository.class);

        when(projectRepository.findByWorkflowId("workflow-1")).thenReturn(Optional.of(new Project()));

        assertThat(
            permissionService(Set.of(), projectRepository)
                .hasResourceScopeIfProjectWorkflow(
                    "workflow-1", WORKSPACE_CONNECTION_ID, CONNECTION, "CONNECTION_EDIT"))
                        .isTrue();
    }

    @Test
    void testConnectionCheckIsSkippedForAWorkflowOutsideAnyProject() {
        authenticate("ana");

        assertThat(
            permissionService(Set.of())
                .hasResourceScopeIfProjectWorkflow("workflow-1", PRIVATE_CONNECTION_ID, CONNECTION,
                    "CONNECTION_EDIT"))
                        .isTrue();
    }

    /**
     * Builds the EE service with a stubbed workspace-scope check that always grants, so any denial in these tests
     * can only come from the visibility precondition.
     */
    private static PermissionServiceImpl permissionService(Set<Long> grantedConnectionIds) {
        return permissionService(grantedConnectionIds, mock(ProjectRepository.class));
    }

    private static PermissionServiceImpl permissionService(
        Set<Long> grantedConnectionIds, ProjectRepository projectRepository) {

        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        WorkspaceScopeCacheService workspaceScopeCacheService = mock(WorkspaceScopeCacheService.class);

        when(workspaceScopeCacheService.getWorkspaceScopes(anyLong(), anyLong()))
            .thenReturn(Set.of("CONNECTION_EDIT", "API_KEY_EDIT"));

        return new PermissionServiceImpl(
            currentUserResolver, mock(PermissionScopeRegistry.class), projectRepository,
            workspaceScopeCacheService, mock(WorkspaceUserRepository.class),
            List.of(connectionOwnershipResolver(), apiKeyOwnershipResolver()),
            List.of(connectionVisibilityProvider()), visibilityResolver(grantedConnectionIds), List.of(),
            new StaticListableBeanFactory().getBeanProvider(ConnectedUserAccessDecider.class),
            mock(EmbeddedScopeCacheService.class), mock(EmbeddedUserRepository.class),
            new StaticListableBeanFactory().getBeanProvider(EmbeddedResourceResolver.class),
            new StaticListableBeanFactory().getBeanProvider(ConnectionService.class));
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
                return id == ORGANIZATION_CONNECTION_ID ? ResourceOwner.unknown()
                    : ResourceOwner.ofWorkspace(WORKSPACE_ID);
            }

            @Override
            public ResourceOwner resolveOwner(Serializable id) {
                return resolveOwner(((Number) id).longValue());
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

                if (id == ORGANIZATION_CONNECTION_ID) {
                    return Optional.of(new VisibilityRecord(id, ResourceVisibility.ORGANIZATION, "admin"));
                }

                return Optional.empty();
            }
        };
    }

    /**
     * The production resolver, with the grant table replaced by {@code grantedIds} read at call time, so a test can
     * grant and revoke between checks.
     */
    private static ResourceVisibilityResolver visibilityResolver(Set<Long> grantedIds) {
        CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

        when(currentUserResolver.fetchCurrentUserId()).thenReturn(OptionalLong.of(7L));

        ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);

        when(resourceGrantService.filterGrantedResourceIds(eq(CONNECTION), eq(7L), anyCollection()))
            .thenAnswer(invocation -> {
                Collection<Long> resourceIds = invocation.getArgument(2);

                return resourceIds.stream()
                    .filter(grantedIds::contains)
                    .collect(Collectors.toSet());
            });

        return new ResourceVisibilityResolverImpl(currentUserResolver, resourceGrantService);
    }
}

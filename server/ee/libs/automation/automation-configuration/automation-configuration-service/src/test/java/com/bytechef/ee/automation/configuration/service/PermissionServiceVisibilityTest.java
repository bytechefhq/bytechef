/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
    private static final long WORKSPACE_ID = 1L;

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
            List.of(connectionVisibilityProvider()), visibilityResolver(grantedConnectionIds));
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
}

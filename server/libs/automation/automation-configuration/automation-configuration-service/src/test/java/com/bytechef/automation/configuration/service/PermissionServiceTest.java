/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class PermissionServiceTest {

    private final PermissionService permissionService =
        new PermissionServiceImpl(mock(UserService.class), List.of(), List.of(), permissiveResolver());

    @BeforeEach
    void setUp() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "user", "user", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER))));

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testHasWorkspaceRoleTrueForAuthenticatedUser() {
        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(99L, "ANY_UNRECOGNIZED_ROLE")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeTrueForAuthenticatedUser() {
        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_DELETE")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(1L, "PROJECT_DELETE")).isTrue();
        assertThat(permissionService.hasWorkspaceScope(1L, "ANY_UNRECOGNIZED_SCOPE")).isTrue();
    }

    @Test
    void testHasWorkspaceScopeForProjectTrueForAuthenticatedUser() {
        assertThat(permissionService.hasWorkspaceScopeForProject(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(1L, "PROJECT_DELETE")).isTrue();
        assertThat(permissionService.hasWorkspaceScopeForProject(1L, "ANY_UNRECOGNIZED_SCOPE")).isTrue();
    }

    @Test
    void testWorkspaceChecksDenyUnauthenticatedCaller() {
        SecurityContextHolder.clearContext();

        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isFalse();
        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isFalse();
        assertThat(permissionService.hasWorkspaceScopeForProject(1L, "WORKFLOW_VIEW")).isFalse();
        assertThat(permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_VIEW")).isFalse();
        assertThat(permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isFalse();
        assertThat(
            permissionService.hasWorkflowScopeIfProjectWorkflow("workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isFalse();
        assertThat(permissionService.hasResourceRole(1L, "project", "ADMIN")).isFalse();
        assertThat(permissionService.isResourceOwner("project", 1L)).isFalse();
    }

    @Test
    void testWorkspaceChecksDenyAnonymousCaller() {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "anonymous", "anonymous", List.of(new SimpleGrantedAuthority(AuthorityConstants.ANONYMOUS))));

        SecurityContextHolder.setContext(securityContext);

        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isFalse();
        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testGetMyWorkspaceScopesReturnsEmpty() {
        // CE has no scope mapping; getMyWorkspaceScopes returns an empty set so client code that relies on the list
        // (e.g., disabling buttons by scope) gracefully degrades rather than dereferencing null.
        assertThat(permissionService.getMyWorkspaceScopes(1L)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceScopesForAnEnvironmentReturnsEmpty() {
        assertThat(permissionService.getMyWorkspaceScopes(1L, Environment.PRODUCTION)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdmin() {
        // CE is single-tenant with permissive workspace access — every authenticated user is effectively ADMIN so
        // EE-branched callers that compare role ordinals (e.g. AiGatewayFacade.validateWorkspaceAccess) don't treat
        // a null response as deny and silently lock non-admin CE users out of EE-branched features.
        assertThat(permissionService.getMyWorkspaceRole(1L)).isEqualTo("ADMIN");
    }

    @Test
    void testEvictWorkspaceScopeCacheIsNoOp() {
        // Should not throw — there is no cache in CE.
        permissionService.evictWorkspaceScopeCache(1L, 2L);
    }

    @Test
    void testEvictAllWorkspaceScopeCacheIsNoOp() {
        permissionService.evictAllWorkspaceScopeCache();
    }

    /**
     * Pins the CE behavior of {@code hasResourceScope}: owner-isolation for owner-carrying resources (PRIVATE),
     * permissive for workspace-mapped resources (shared within the single CE workspace), fail-closed when nothing is
     * resolvable. Also pins {@code isResourceOwner} (permissive — EE-only enforcement for now).
     */
    @Nested
    class Resource {

        private final UserService userService = mock(UserService.class);

        private PermissionService createService(ResourceOwnershipResolver... resolvers) {
            return new PermissionServiceImpl(userService, List.of(resolvers), List.of(), permissiveResolver());
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
        void testHasResourceScopeOwnerMatchAllowsInCe() {
            User user = new User();

            user.setId(7L);

            when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

            PermissionService service = createService(resolver("Connection", ResourceOwner.ofUser(7L)));

            assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isTrue();
        }

        @Test
        void testHasResourceScopeOwnerMismatchDeniesInCe() {
            User user = new User();

            user.setId(99L);

            when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

            PermissionService service = createService(resolver("Connection", ResourceOwner.ofUser(7L)));

            assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
        }

        @Test
        void testVisibilityBearingResourceDropsOwnerIsolationInCe() {
            // CE creates every connection WORKSPACE-visible, so a colleague who can see it in the list must also be
            // able to act on it by id. Owner-isolation is replaced by visibility for types that registered a
            // provider — and only for those, which the preceding test pins for everything else.
            User user = new User();

            user.setId(99L);

            when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

            PermissionService service = new PermissionServiceImpl(
                userService, List.of(resolver("Connection", ResourceOwner.ofUser(7L))),
                List.of(visibilityProvider("Connection")), permissiveResolver());

            assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isTrue();
        }

        private static ResourceVisibilityProvider visibilityProvider(String resourceType) {
            return new ResourceVisibilityProvider() {

                @Override
                public String resourceType() {
                    return resourceType;
                }

                @Override
                public Optional<VisibilityRecord> fetchVisibility(long id) {
                    return Optional.of(
                        new VisibilityRecord(id, ResourceVisibility.WORKSPACE, "someone-else"));
                }
            };
        }

        @Test
        void testHasResourceScopeNoOwnerFailsClosedInCe() {
            PermissionService service = createService(resolver("Connection", ResourceOwner.unknown()));

            assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
        }

        @Test
        void testHasResourceScopeWorkspaceMappedIsPermissiveInCe() {
            // A workspace-mapped resource with no owner user (knowledge bases, data tables, workspaces, projects,
            // workflows, ...) is shared within the single CE workspace, so CE is permissive.
            PermissionService service = createService(resolver("KnowledgeBase", ResourceOwner.ofWorkspace(42L)));

            assertThat(service.hasResourceScope(1L, "KnowledgeBase", "KNOWLEDGE_BASE_EDIT")).isTrue();
        }

        @Test
        void testHasResourceScopeUnregisteredTypeFailsClosed() {
            PermissionService service = createService();

            assertThat(service.hasResourceScope(1L, "Nope", "X")).isFalse();
        }

        @Test
        void testIsResourceOwnerPermissiveInCe() {
            PermissionService service = createService(resolver("ApiKey", ResourceOwner.ofUser(7L)));

            assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
        }

        @Test
        void testHasResourceRolePermissiveInCe() {
            PermissionService service = createService(resolver("KnowledgeBase", ResourceOwner.ofWorkspace(42L)));

            assertThat(service.hasResourceRole(1L, "KnowledgeBase", "EDITOR")).isTrue();
        }

        @Test
        void testHasWorkflowScopePermissiveInCe() {
            PermissionService service = createService();

            assertThat(service.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
        }

        @Test
        void testHasResourceScopeDeniesUnauthenticatedCaller() {
            SecurityContextHolder.clearContext();

            PermissionService service = createService(resolver("Connection", ResourceOwner.ofUser(7L)));

            assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
        }
    }

    /**
     * A resolver that hides nothing, so these tests exercise the ownership and scope logic rather than visibility.
     * Visibility gating has its own tests.
     */
    private static ResourceVisibilityResolver permissiveResolver() {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .collect(Collectors.toSet());
    }
}

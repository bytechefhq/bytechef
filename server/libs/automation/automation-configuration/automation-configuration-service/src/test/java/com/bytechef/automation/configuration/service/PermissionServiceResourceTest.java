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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the CE behavior of {@code hasResourceScope}: owner-isolation for owner-carrying resources (PRIVATE), permissive
 * for workspace-mapped resources (shared within the single CE workspace), fail-closed when nothing is resolvable. Also
 * pins {@code isResourceOwner} (permissive — EE-only enforcement for now).
 *
 * @author Ivica Cardic
 */
class PermissionServiceResourceTest {

    private final UserService userService = Mockito.mock(UserService.class);

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

    private PermissionService permissionService(ResourceOwnershipResolver... resolvers) {
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

        PermissionService service = permissionService(resolver("Connection", ResourceOwner.ofUser(7L)));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeOwnerMismatchDeniesInCe() {
        User user = new User();

        user.setId(99L);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        PermissionService service = permissionService(resolver("Connection", ResourceOwner.ofUser(7L)));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testVisibilityBearingResourceDropsOwnerIsolationInCe() {
        // CE creates every connection WORKSPACE-visible, so a colleague who can see it in the list must also be
        // able to act on it by id. Owner-isolation is replaced by visibility for types that registered a provider —
        // and only for those, which the preceding test pins for everything else.
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
        PermissionService service = permissionService(resolver("Connection", ResourceOwner.unknown()));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeWorkspaceMappedIsPermissiveInCe() {
        // A workspace-mapped resource with no owner user (knowledge bases, data tables, workspaces, projects,
        // workflows, ...) is shared within the single CE workspace, so CE is permissive.
        PermissionService service = permissionService(resolver("KnowledgeBase", ResourceOwner.ofWorkspace(42L)));

        assertThat(service.hasResourceScope(1L, "KnowledgeBase", "KNOWLEDGE_BASE_EDIT")).isTrue();
    }

    @Test
    void testHasResourceScopeUnregisteredTypeFailsClosed() {
        PermissionService service = permissionService();

        assertThat(service.hasResourceScope(1L, "Nope", "X")).isFalse();
    }

    @Test
    void testIsResourceOwnerPermissiveInCe() {
        PermissionService service = permissionService(resolver("ApiKey", ResourceOwner.ofUser(7L)));

        assertThat(service.isResourceOwner("ApiKey", 1L)).isTrue();
    }

    @Test
    void testHasResourceRolePermissiveInCe() {
        PermissionService service = permissionService(resolver("KnowledgeBase", ResourceOwner.ofWorkspace(42L)));

        assertThat(service.hasResourceRole(1L, "KnowledgeBase", "EDITOR")).isTrue();
    }

    @Test
    void testHasWorkflowScopePermissiveInCe() {
        PermissionService service = permissionService();

        assertThat(service.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasResourceScopeDeniesUnauthenticatedCaller() {
        SecurityContextHolder.clearContext();

        PermissionService service = permissionService(resolver("Connection", ResourceOwner.ofUser(7L)));

        assertThat(service.hasResourceScope(1L, "Connection", "CONNECTION_DELETE")).isFalse();
    }

    /**
     * A resolver that hides nothing, so these tests exercise ownership resolution rather than visibility. The
     * visibility precondition has its own test class.
     */
    private static ResourceVisibilityResolver permissiveResolver() {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .collect(Collectors.toSet());
    }
}

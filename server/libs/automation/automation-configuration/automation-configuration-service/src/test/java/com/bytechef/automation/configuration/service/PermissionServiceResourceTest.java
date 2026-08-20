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
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void testStringIdOnNumericProviderFailsClosedInCe() {
        // The ownership resolver answers for a String id too, so the owner branch would return true on its own.
        // That leaves the visibility precondition as the only thing that can produce false: if the default
        // fetchVisibility(Serializable) stopped failing closed on a non-numeric id, this test would go green.
        PermissionService service = new PermissionServiceImpl(
            userService, List.of(anyIdResolver("Connection", ResourceOwner.ofUser(7L))),
            List.of(visibilityProvider("Connection")), permissiveResolver());

        assertThat(service.hasResourceScope("abc", "Connection", "CONNECTION_DELETE")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectHonoursVisibilityInCe() {
        // The project resolves to a workspace, so the has-a-provider branch of hasResourceScope would return true on
        // its own — as the pre-change body (isAuthenticated()) also did. The visibility precondition is therefore the
        // only thing that can produce false here.
        User user = new User();

        user.setId(99L);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        ResourceVisibilityResolver denyAll = (resourceType, workspaceId, candidates) -> Set.of();

        PermissionService service = new PermissionServiceImpl(
            userService, List.of(resolver("Project", ResourceOwner.ofWorkspace(1L))),
            List.of(privateProjectVisibilityProvider()), denyAll);

        assertThat(service.hasWorkspaceScopeForProject(1L, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testHasWorkspaceScopeForProjectWithEnvironmentHonoursVisibilityInCe() {
        // Same wiring as the environment-unaware test above, and for the same reason: everything except visibility
        // permits. CE's environment-aware overload delegates to the same chokepoint, so the two cannot disagree
        // about one project.
        User user = new User();

        user.setId(99L);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        ResourceVisibilityResolver denyAll = (resourceType, workspaceId, candidates) -> Set.of();

        PermissionService service = new PermissionServiceImpl(
            userService, List.of(resolver("Project", ResourceOwner.ofWorkspace(1L))),
            List.of(privateProjectVisibilityProvider()), denyAll);

        assertThat(service.hasWorkspaceScopeForProject(1L, "WORKFLOW_VIEW", Environment.PRODUCTION)).isFalse();
    }

    /**
     * Unlike {@link #resolver(String, ResourceOwner)}, this one answers for a non-numeric id as well, instead of
     * inheriting the SPI default that fails closed for it.
     */
    private static ResourceOwnershipResolver anyIdResolver(String type, ResourceOwner owner) {
        return new ResourceOwnershipResolver() {
            @Override
            public String resourceType() {
                return type;
            }

            @Override
            public ResourceOwner resolveOwner(long id) {
                return owner;
            }

            @Override
            public ResourceOwner resolveOwner(Serializable id) {
                return owner;
            }
        };
    }

    /**
     * A project nobody but its creator can see, shared by both project-keyed tests so the two overloads are pinned
     * against identical wiring.
     */
    private static ResourceVisibilityProvider privateProjectVisibilityProvider() {
        return new ResourceVisibilityProvider() {

            @Override
            public String resourceType() {
                return "Project";
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                return Optional.of(new VisibilityRecord(id, ResourceVisibility.PRIVATE, "someone-else"));
            }
        };
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
    void testHasWorkflowScopeWorkspaceMappedIsPermissiveInCe() {
        // CE is permissive for a workflow that RESOLVES — one whose ownership resolver answers, mapping it to a
        // workspace and no owner user. It is not permissive unconditionally: hasWorkflowScope routes through
        // hasResourceScope, so an unregistered or non-answering resolver fails closed, which is why the Workflow
        // ownership resolver has to be registered for this to pass.
        PermissionService service = new PermissionServiceImpl(
            userService, List.of(anyIdResolver("Workflow", ResourceOwner.ofWorkspace(1L))), List.of(),
            permissiveResolver());

        assertThat(service.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasWorkflowScopeHonoursTheProjectsVisibilityInCe() {
        // Both halves register the same ownership resolver, which answers for a String id and resolves to a
        // workspace — so the ownership branch permits in both, and the resolver's verdict on the owning project is
        // the only difference between them. Remove the precondition from hasWorkflowScope and the first assertion
        // goes green for the wrong reason.
        ResourceVisibilityResolver denyAll = (resourceType, workspaceId, candidates) -> Set.of();

        PermissionService hidden = new PermissionServiceImpl(
            userService, List.of(anyIdResolver("Workflow", ResourceOwner.ofWorkspace(1L))),
            List.of(workflowVisibilityProvider()), denyAll);

        assertThat(hidden.hasWorkflowScope("wf-uuid", "WORKFLOW_VIEW"))
            .as("a workflow inside a project the caller cannot see must be denied")
            .isFalse();

        PermissionService visible = new PermissionServiceImpl(
            userService, List.of(anyIdResolver("Workflow", ResourceOwner.ofWorkspace(1L))),
            List.of(workflowVisibilityProvider()), permissiveResolver());

        assertThat(visible.hasWorkflowScope("wf-uuid", "WORKFLOW_VIEW"))
            .as("the same workflow is permitted once its project is visible")
            .isTrue();
    }

    /**
     * Mirrors the production {@code WorkflowVisibilityProvider}: a workflow is keyed by a String and inherits the
     * record of the project that owns it, so grants resolve under {@code "Project"}.
     */
    private static ResourceVisibilityProvider workflowVisibilityProvider() {
        return new ResourceVisibilityProvider() {

            @Override
            public String resourceType() {
                return "Workflow";
            }

            @Override
            public String visibilityResourceType() {
                return "Project";
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(long id) {
                return Optional.empty();
            }

            @Override
            public Optional<VisibilityRecord> fetchVisibility(Serializable id) {
                if (!(id instanceof String)) {
                    return Optional.empty();
                }

                return Optional.of(new VisibilityRecord(20L, ResourceVisibility.PRIVATE, "someone-else"));
            }
        };
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

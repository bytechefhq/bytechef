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

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        new PermissionServiceImpl(mock(UserService.class), List.of());

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
}

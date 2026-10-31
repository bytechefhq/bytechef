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

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class PermissionServiceIntTest {

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        setAuthentication("user", AuthorityConstants.USER);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testTheCommunityEditionImplementationIsTheActiveBean() {
        assertThat(permissionService).isInstanceOf(PermissionServiceImpl.class);
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
    void testHasResourceScopeUsesTheRegisteredOwnershipResolvers() {
        assertThat(permissionService.hasResourceScope(1L, "Workspace", "WORKSPACE_VIEW")).isTrue();
        assertThat(permissionService.hasResourceScope(1L, "UnknownResourceType", "WORKSPACE_VIEW")).isFalse();
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
        assertThat(permissionService.hasResourceScope(1L, "Workspace", "WORKSPACE_VIEW")).isFalse();
    }

    @Test
    void testWorkspaceChecksDenyAnonymousCaller() {
        setAuthentication("anonymous", AuthorityConstants.ANONYMOUS);

        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isFalse();
        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isFalse();
    }

    @Test
    void testGetMyWorkspaceScopesReturnsEmpty() {
        assertThat(permissionService.getMyWorkspaceScopes(1L)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdmin() {
        assertThat(permissionService.getMyWorkspaceRole(1L)).isEqualTo("ADMIN");
    }

    @Test
    void testEvictWorkspaceScopeCacheIsNoOp() {
        permissionService.evictWorkspaceScopeCache(1L, 2L);

        assertThat(permissionService.hasWorkspaceScope(2L, "WORKFLOW_VIEW")).isTrue();
    }

    @Test
    void testEvictAllWorkspaceScopeCacheIsNoOp() {
        permissionService.evictAllWorkspaceScopeCache();

        assertThat(permissionService.hasWorkspaceScope(1L, "WORKFLOW_VIEW")).isTrue();
    }

    private static void setAuthentication(String username, String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(username, username, List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);
    }
}

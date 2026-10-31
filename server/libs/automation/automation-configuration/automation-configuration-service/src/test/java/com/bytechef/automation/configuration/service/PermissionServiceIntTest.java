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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.WorkspaceApiKeyIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the CE behavior of {@link PermissionServiceImpl} against the real {@code ResourceOwnershipResolver} beans and
 * persisted rows: workspace checks are permissive for any authenticated caller, {@code hasResourceScope} enforces owner
 * isolation for owner-carrying resources, is permissive for workspace-mapped resources, and fails closed when nothing
 * is resolvable.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@WorkspaceApiKeyIntTestConfigurationSharedMocks
class PermissionServiceIntTest {

    private static final long OWNER_USER_ID = 7L;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long apiKeyId;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        reset(userService);

        setAuthentication("user", AuthorityConstants.USER);

        ApiKey apiKey = new ApiKey();

        apiKey.setName("permission-service");
        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setUserId(OWNER_USER_ID);

        apiKeyId = apiKeyService.create(apiKey)
            .getId();

        Workspace workspace = workspaceRepository.save(new Workspace("permission-service"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        apiKeyRepository.deleteById(apiKeyId);
        workspaceRepository.deleteById(workspaceId);
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

    @Test
    void testHasResourceScopeOwnerMatchAllowsInCe() {
        setCurrentUser(OWNER_USER_ID);

        assertThat(permissionService.hasResourceScope(apiKeyId, "ApiKey", "API_KEY_DELETE")).isTrue();
    }

    @Test
    void testHasResourceScopeOwnerMismatchDeniesInCe() {
        setCurrentUser(99L);

        assertThat(permissionService.hasResourceScope(apiKeyId, "ApiKey", "API_KEY_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeNoOwnerFailsClosedInCe() {
        setCurrentUser(OWNER_USER_ID);

        assertThat(permissionService.hasResourceScope(Long.MAX_VALUE, "ApiKey", "API_KEY_DELETE")).isFalse();
    }

    @Test
    void testHasResourceScopeWorkspaceMappedIsPermissiveInCe() {
        assertThat(permissionService.hasResourceScope(workspaceId, "Workspace", "WORKSPACE_VIEW")).isTrue();
    }

    @Test
    void testHasResourceScopeUnregisteredTypeFailsClosed() {
        assertThat(permissionService.hasResourceScope(1L, "Nope", "X")).isFalse();
    }

    @Test
    void testIsResourceOwnerPermissiveInCe() {
        assertThat(permissionService.isResourceOwner("ApiKey", apiKeyId)).isTrue();
    }

    @Test
    void testHasResourceRolePermissiveInCe() {
        assertThat(permissionService.hasResourceRole(workspaceId, "Workspace", "EDITOR")).isTrue();
    }

    @Test
    void testHasWorkflowScopePermissiveInCe() {
        assertThat(permissionService.hasWorkflowScope("wf-uuid", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testHasResourceScopeDeniesUnauthenticatedCaller() {
        setCurrentUser(OWNER_USER_ID);

        SecurityContextHolder.clearContext();

        assertThat(permissionService.hasResourceScope(apiKeyId, "ApiKey", "API_KEY_DELETE")).isFalse();
    }

    private void setCurrentUser(long userId) {
        User user = new User();

        user.setId(userId);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));
    }

    private static void setAuthentication(String username, String authority) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(username, username,
                List.of(new SimpleGrantedAuthority(authority))));

        SecurityContextHolder.setContext(securityContext);
    }
}

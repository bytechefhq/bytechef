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

import com.bytechef.platform.user.service.UserService;
import org.junit.jupiter.api.Test;

/**
 * Pins the no-op contract of the CE {@link PermissionServiceImpl}. RBAC is an EE feature; in CE every authorization
 * check must succeed so that {@code @PreAuthorize} annotations on shared services do not lock CE users out. If anyone
 * ever inverts a {@code return true} to {@code return false} in CE, every protected method silently breaks &mdash; this
 * test is the regression net for that scenario.
 *
 * @author Ivica Cardic
 */
class PermissionServiceTest {

    private final PermissionService permissionService = new PermissionServiceImpl(mock(UserService.class));

    @Test
    void testHasWorkspaceRoleAlwaysTrue() {
        assertThat(permissionService.hasWorkspaceRole(1L, "ADMIN")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "EDITOR")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(1L, "VIEWER")).isTrue();
        assertThat(permissionService.hasWorkspaceRole(99L, "ANY_UNRECOGNIZED_ROLE")).isTrue();
    }

    @Test
    void testHasProjectScopeAlwaysTrue() {
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_VIEW")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "WORKFLOW_DELETE")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "PROJECT_DELETE")).isTrue();
        assertThat(permissionService.hasProjectScope(1L, "ANY_UNRECOGNIZED_SCOPE")).isTrue();
    }

    @Test
    void testHasProjectRoleAlwaysTrue() {
        assertThat(permissionService.hasProjectRole(1L, "ADMIN")).isTrue();
        assertThat(permissionService.hasProjectRole(1L, "VIEWER")).isTrue();
    }

    @Test
    void testGetMyProjectScopesReturnsEmpty() {
        // CE has no scope mapping; getMyProjectScopes returns an empty set so client code that relies on the list
        // (e.g., disabling buttons by scope) gracefully degrades rather than dereferencing null.
        assertThat(permissionService.getMyProjectScopes(1L)).isEmpty();
    }

    @Test
    void testGetMyWorkspaceRoleReturnsAdmin() {
        // CE is single-tenant with permissive workspace access — every authenticated user is effectively ADMIN so
        // EE-branched callers that compare role ordinals (e.g. AiGatewayFacade.validateWorkspaceAccess) don't treat
        // a null response as deny and silently lock non-admin CE users out of EE-branched features.
        assertThat(permissionService.getMyWorkspaceRole(1L)).isEqualTo("ADMIN");
    }

    @Test
    void testEvictProjectScopeCacheIsNoOp() {
        // Should not throw — there is no cache in CE.
        permissionService.evictProjectScopeCache(1L, 2L);
    }

    @Test
    void testEvictAllProjectScopeCacheIsNoOp() {
        permissionService.evictAllProjectScopeCache();
    }
}

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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacadeImpl;
import com.bytechef.automation.configuration.facade.ProjectTagFacadeImpl;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on the workspace tag listings through the real expression
 * handler, in both directions, and shows that a scope held in another environment does not reach a listing asked for
 * one environment.
 *
 * @author Ivica Cardic
 */
class WorkspaceTagListingAuthorizationTest {

    private static final long WORKSPACE_ID = 42L;

    @Test
    void testConnectionTagsRequireTheConnectionViewScopeInTheRequestedEnvironment() throws Exception {
        Method method = WorkspaceConnectionFacadeImpl.class.getMethod("getConnectionTags", long.class, Long.class);

        assertThat(evaluateEnvironmentGuard(method, "CONNECTION_VIEW", Environment.PRODUCTION)).isTrue();
        assertThat(evaluateEnvironmentGuard(method, "CONNECTION_VIEW", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testProjectDeploymentTagsRequireTheDeploymentViewScopeInTheRequestedEnvironment() throws Exception {
        Method method = ProjectDeploymentFacadeImpl.class.getMethod(
            "getProjectDeploymentTags", long.class, Long.class);

        assertThat(evaluateEnvironmentGuard(method, "DEPLOYMENT_VIEW", Environment.PRODUCTION)).isTrue();
        assertThat(evaluateEnvironmentGuard(method, "DEPLOYMENT_VIEW", Environment.DEVELOPMENT)).isFalse();
    }

    @Test
    void testProjectTagsRequireTheWorkflowViewScopeInTheWorkspace() throws Exception {
        Method method = ProjectTagFacadeImpl.class.getMethod("getProjectTags", long.class);

        PermissionService grantingPermissionService = mock(PermissionService.class);

        when(grantingPermissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        assertThat(
            ProjectWorkflowFacadeAuthorizationTest.evaluateGuard(
                grantingPermissionService, method, new Object[] {
                    WORKSPACE_ID
                }))
                    .isTrue();
        assertThat(
            ProjectWorkflowFacadeAuthorizationTest.evaluateGuard(
                mock(PermissionService.class), method, new Object[] {
                    WORKSPACE_ID
                }))
                    .isFalse();
    }

    private static boolean evaluateEnvironmentGuard(Method method, String scope, Environment grantedEnvironment) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, scope, grantedEnvironment)).thenReturn(true);

        return ProjectWorkflowFacadeAuthorizationTest.evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, (long) Environment.PRODUCTION.ordinal()
        });
    }
}

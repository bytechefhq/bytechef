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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutomationPermissionEvaluatorTest {

    private PermissionService permissionService;
    private AutomationPermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        evaluator = new AutomationPermissionEvaluator(permissionService);
    }

    @Test
    void testNumericResourceTypeDelegatesToHasResourceScope() {
        when(permissionService.hasResourceScope(5L, "Connection", "CONNECTION_DELETE")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 5L, "Connection", "CONNECTION_DELETE")).isTrue();
    }

    @Test
    void testWorkspaceTokenDelegatesToHasResourceScope() {
        when(permissionService.hasResourceScope(3L, "Workspace", "WORKFLOW_VIEW")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 3L, "Workspace", "WORKFLOW_VIEW")).isTrue();
    }

    @Test
    void testStringWorkflowIdDelegatesToHasResourceScope() {
        // The String workflow UUID flows through the same hasResourceScope(Serializable, ...) path.
        when(permissionService.hasResourceScope("workflow-1", "Workflow", "WORKFLOW_EDIT")).thenReturn(true);

        assertThat(evaluator.hasPermission(null, "workflow-1", "Workflow", "WORKFLOW_EDIT")).isTrue();
    }

    @Test
    void testDeniedWhenHasResourceScopeReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 9L, "Bogus", "X")).isFalse();
    }

    @Test
    void testTwoArgFormAlwaysDeniesAndNeverTouchesPermissionService() {
        // The former hasPermission('Tenant', 'ADMIN') tenant-admin check is now the isTenantAdmin() SpEL built-in on
        // AutomationMethodSecurityExpressionRoot, so the two-argument overload no longer authorizes anything.
        assertThat(evaluator.hasPermission(null, "Tenant", "ADMIN")).isFalse();
        assertThat(evaluator.hasPermission(null, "Something", "ADMIN")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testSkipChecksShortCircuitsWithoutTouchingPermissionService() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 1L, "Project", "PROJECT_DELETE")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }
}

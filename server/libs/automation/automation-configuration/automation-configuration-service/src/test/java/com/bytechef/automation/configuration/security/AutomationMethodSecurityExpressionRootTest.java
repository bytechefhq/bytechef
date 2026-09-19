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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class AutomationMethodSecurityExpressionRootTest {

    private static final long RESOURCE_ID = 42L;
    private static final String RESOURCE_TYPE = "Connection";
    private static final String SCOPE = "CONNECTION_EDIT";
    private static final String WORKFLOW_ID = "workflow-7";
    private static final long WORKSPACE_ID = 1051L;

    private PermissionService permissionService;
    private AutomationMethodSecurityExpressionRoot root;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);

        Supplier<Authentication> authentication = () -> mock(Authentication.class);
        MethodInvocation methodInvocation = mock(MethodInvocation.class);

        root = new AutomationMethodSecurityExpressionRoot(authentication, methodInvocation, permissionService);
    }

    @Test
    void testIsCurrentUserDelegates() {
        when(permissionService.isCurrentUser(7L)).thenReturn(true);

        assertThat(root.isCurrentUser(7L)).isTrue();
    }

    @Test
    void testIsTenantAdminDelegates() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(root.isTenantAdmin()).isTrue();
    }

    @Test
    void testIsResourceOwnerDelegates() {
        when(permissionService.isResourceOwner("ApiKey", 9L)).thenReturn(true);

        assertThat(root.isResourceOwner(9L, "ApiKey")).isTrue();
    }

    @Test
    void testIsCurrentUserShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isCurrentUser(7L)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsTenantAdminShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isTenantAdmin()).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsResourceOwnerShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isResourceOwner(9L, "ApiKey")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentDelegatesWithTheEnvironment() {
        when(permissionService.hasWorkflowScope("workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .thenReturn(true);

        assertThat(root.hasWorkflowScopeInEnvironment("workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
            .isTrue();
        assertThat(root.hasWorkflowScopeInEnvironment("workflow-1", "WORKFLOW_EDIT", Environment.PRODUCTION))
            .isFalse();
    }

    @Test
    void testHasWorkflowScopeInEnvironmentShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkflowScopeInEnvironment("workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentDelegatesWithTheEnvironment() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow("workflow-1", "WORKFLOW_EDIT", Environment.STAGING))
            .thenReturn(true);

        assertThat(
            root.hasWorkflowScopeIfProjectWorkflowInEnvironment("workflow-1", "WORKFLOW_EDIT", Environment.STAGING))
                .isTrue();
        assertThat(
            root.hasWorkflowScopeIfProjectWorkflowInEnvironment(
                "workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                    .isFalse();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdResolvesTheOrdinal() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow("workflow-1", "WORKFLOW_EDIT", Environment.PRODUCTION))
            .thenReturn(true);

        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId("workflow-1", "WORKFLOW_EDIT", 2L)).isTrue();
        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId("workflow-1", "WORKFLOW_EDIT", 0L)).isFalse();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdTreatsNullAsDevelopment() {
        when(
            permissionService.hasWorkflowScopeIfProjectWorkflow("workflow-1", "WORKFLOW_EDIT", Environment.DEVELOPMENT))
                .thenReturn(true);

        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId("workflow-1", "WORKFLOW_EDIT", null))
            .isTrue();
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdDeniesAnOrdinalOutsideTheEnum() {
        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId("workflow-1", "WORKFLOW_EDIT", 3L)).isFalse();
        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId("workflow-1", "WORKFLOW_EDIT", -1L))
            .isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId("workflow-1", "WORKFLOW_EDIT", 3L))
                .isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEveryEnvironmentPassesArgumentsAndResultThrough() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE)).thenReturn(true);

        assertThat(root.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE)).isTrue();

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEveryEnvironmentReturnsFalseWhenDenied() {
        when(permissionService.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE)).thenReturn(false);

        assertThat(root.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE)).isFalse();

        verify(permissionService).hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE);
    }

    @Test
    void testHasWorkspaceScopeInEveryEnvironmentShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkspaceScopeInEveryEnvironment(WORKSPACE_ID, SCOPE)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentPassesArgumentsAndResultThrough() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.STAGING)).thenReturn(true);

        assertThat(root.hasWorkspaceScopeInEnvironment(WORKSPACE_ID, SCOPE, Environment.STAGING)).isTrue();

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.STAGING);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentReturnsFalseWhenDenied() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.PRODUCTION)).thenReturn(false);

        assertThat(root.hasWorkspaceScopeInEnvironment(WORKSPACE_ID, SCOPE, Environment.PRODUCTION)).isFalse();

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkspaceScopeInEnvironment(WORKSPACE_ID, SCOPE, Environment.PRODUCTION)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdKeepsTheEnvironmentUnawareCheckForNull() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, SCOPE)).thenReturn(true);

        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, null)).isTrue();

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, SCOPE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdReturnsFalseWhenTheEnvironmentUnawareCheckDenies() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, SCOPE)).thenReturn(false);

        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, null)).isFalse();

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, SCOPE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdResolvesTheOrdinal() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.STAGING)).thenReturn(true);

        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 1L)).isTrue();

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.STAGING);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdReturnsFalseWhenTheResolvedEnvironmentDenies() {
        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.PRODUCTION)).thenReturn(false);

        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 2L)).isFalse();

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, SCOPE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdDeniesAnOrdinalOutsideTheEnum() {
        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, -1L)).isFalse();
        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, (long) Environment.values().length))
            .isFalse();
        assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, Long.MAX_VALUE)).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkspaceScopeInEnvironmentIdShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, 1L)).isTrue();
            assertThat(root.hasWorkspaceScopeInEnvironmentId(WORKSPACE_ID, SCOPE, null)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasResourceScopeInEnvironmentKeepsTheEnvironmentUnawareCheckForNull() {
        when(permissionService.hasResourceScope(RESOURCE_ID, RESOURCE_TYPE, SCOPE)).thenReturn(true);

        assertThat(root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null)).isTrue();

        verify(permissionService).hasResourceScope(RESOURCE_ID, RESOURCE_TYPE, SCOPE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasResourceScopeInEnvironmentReturnsFalseWhenTheEnvironmentUnawareCheckDenies() {
        when(permissionService.hasResourceScope(RESOURCE_ID, RESOURCE_TYPE, SCOPE)).thenReturn(false);

        assertThat(root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null)).isFalse();

        verify(permissionService).hasResourceScope(RESOURCE_ID, RESOURCE_TYPE, SCOPE);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasResourceScopeInEnvironmentPassesArgumentsAndResultThrough() {
        when(permissionService.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.STAGING))
            .thenReturn(true);

        assertThat(root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.STAGING))
            .isTrue();

        verify(permissionService).hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.STAGING);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasResourceScopeInEnvironmentReturnsFalseWhenDenied() {
        when(
            permissionService.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION))
                .thenReturn(false);

        assertThat(root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION))
            .isFalse();

        verify(permissionService).hasResourceScopeInEnvironment(
            RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasResourceScopeInEnvironmentShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, Environment.PRODUCTION))
                .isTrue();
            assertThat(root.hasResourceScopeInEnvironment(RESOURCE_ID, RESOURCE_TYPE, SCOPE, null)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeInEnvironmentPassesExactArguments() {
        when(permissionService.hasWorkflowScope(WORKFLOW_ID, SCOPE, Environment.STAGING)).thenReturn(false);

        assertThat(root.hasWorkflowScopeInEnvironment(WORKFLOW_ID, SCOPE, Environment.STAGING)).isFalse();

        verify(permissionService).hasWorkflowScope(WORKFLOW_ID, SCOPE, Environment.STAGING);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentPassesExactArguments() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, SCOPE, Environment.PRODUCTION))
            .thenReturn(true);

        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironment(WORKFLOW_ID, SCOPE, Environment.PRODUCTION))
            .isTrue();

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, SCOPE, Environment.PRODUCTION);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(
                root.hasWorkflowScopeIfProjectWorkflowInEnvironment(WORKFLOW_ID, SCOPE, Environment.PRODUCTION))
                    .isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdPassesExactArguments() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, SCOPE, Environment.STAGING))
            .thenReturn(true);

        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, SCOPE, 1L)).isTrue();

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, SCOPE, Environment.STAGING);
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void testHasWorkflowScopeIfProjectWorkflowInEnvironmentIdReturnsFalseForNullWhenDevelopmentDenies() {
        when(permissionService.hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, SCOPE, Environment.DEVELOPMENT))
            .thenReturn(false);

        assertThat(root.hasWorkflowScopeIfProjectWorkflowInEnvironmentId(WORKFLOW_ID, SCOPE, null)).isFalse();

        verify(permissionService).hasWorkflowScopeIfProjectWorkflow(WORKFLOW_ID, SCOPE, Environment.DEVELOPMENT);
        verifyNoMoreInteractions(permissionService);
    }
}

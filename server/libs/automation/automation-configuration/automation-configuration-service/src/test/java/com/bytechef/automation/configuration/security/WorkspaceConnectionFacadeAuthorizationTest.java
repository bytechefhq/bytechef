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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link WorkspaceConnectionFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @author Ivica Cardic
 */
class WorkspaceConnectionFacadeAuthorizationTest {

    private static final long CONNECTION_ID = 7L;
    private static final String CONNECTION_TYPE = "Connection";
    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 42L;

    @Test
    void testGetConnectionDeniesWhenTheConnectionViewScopeIsRefused() throws Exception {
        assertResourceGuard(getConnectionMethod(), new Object[] {
            CONNECTION_ID
        }, "CONNECTION_VIEW", false);
    }

    @Test
    void testGetConnectionAllowsWhenTheConnectionViewScopeIsGranted() throws Exception {
        assertResourceGuard(getConnectionMethod(), new Object[] {
            CONNECTION_ID
        }, "CONNECTION_VIEW", true);
    }

    @Test
    void testGetConnectionDeniesTheOwnerWithoutTheConnectionViewScope() throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isResourceOwner(CONNECTION_TYPE, CONNECTION_ID)).thenReturn(true);

        assertThat(evaluateGuard(permissionService, getConnectionMethod(), new Object[] {
            CONNECTION_ID
        })).isFalse();
    }

    @Test
    void testDeleteAllowsTheOwnerHoldingTheScope() throws Exception {
        assertScopeAndOwnerGuard(deleteMethod(), new Object[] {
            CONNECTION_ID
        }, "CONNECTION_DELETE", true, true, false, true);
    }

    @Test
    void testDeleteDeniesTheOwnerWithoutTheScope() throws Exception {
        assertScopeAndOwnerGuard(deleteMethod(), new Object[] {
            CONNECTION_ID
        }, "CONNECTION_DELETE", false, true, false, false);
    }

    @Test
    void testDeleteDeniesAScopeHolderWhoIsNotTheOwner() throws Exception {
        assertScopeAndOwnerGuard(deleteMethod(), new Object[] {
            CONNECTION_ID
        }, "CONNECTION_DELETE", true, false, false, false);
    }

    @Test
    void testDeleteAllowsTheTenantAdmin() throws Exception {
        assertScopeAndOwnerGuard(deleteMethod(), new Object[] {
            CONNECTION_ID
        }, "CONNECTION_DELETE", true, false, true, true);
    }

    @Test
    void testUpdateAllowsTheOwnerHoldingTheScope() throws Exception {
        assertScopeAndOwnerGuard(updateMethod(), new Object[] {
            CONNECTION_ID, "name", List.of(), 1
        }, "CONNECTION_EDIT", true, true, false, true);
    }

    @Test
    void testUpdateDeniesTheOwnerWithoutTheScope() throws Exception {
        assertScopeAndOwnerGuard(updateMethod(), new Object[] {
            CONNECTION_ID, "name", List.of(), 1
        }, "CONNECTION_EDIT", false, true, false, false);
    }

    @Test
    void testUpdateDeniesAScopeHolderWhoIsNotTheOwner() throws Exception {
        assertScopeAndOwnerGuard(updateMethod(), new Object[] {
            CONNECTION_ID, "name", List.of(), 1
        }, "CONNECTION_EDIT", true, false, false, false);
    }

    @Test
    void testUpdateAllowsTheTenantAdmin() throws Exception {
        assertScopeAndOwnerGuard(updateMethod(), new Object[] {
            CONNECTION_ID, "name", List.of(), 1
        }, "CONNECTION_EDIT", true, false, true, true);
    }

    @Test
    void testUpdateTagsAllowsTheOwnerHoldingTheScope() throws Exception {
        assertScopeAndOwnerGuard(updateTagsMethod(), new Object[] {
            CONNECTION_ID, List.of()
        }, "CONNECTION_EDIT", true, true, false, true);
    }

    @Test
    void testUpdateTagsDeniesTheOwnerWithoutTheScope() throws Exception {
        assertScopeAndOwnerGuard(updateTagsMethod(), new Object[] {
            CONNECTION_ID, List.of()
        }, "CONNECTION_EDIT", false, true, false, false);
    }

    @Test
    void testUpdateTagsDeniesAScopeHolderWhoIsNotTheOwner() throws Exception {
        assertScopeAndOwnerGuard(updateTagsMethod(), new Object[] {
            CONNECTION_ID, List.of()
        }, "CONNECTION_EDIT", true, false, false, false);
    }

    @Test
    void testUpdateTagsAllowsTheTenantAdmin() throws Exception {
        assertScopeAndOwnerGuard(updateTagsMethod(), new Object[] {
            CONNECTION_ID, List.of()
        }, "CONNECTION_EDIT", true, false, true, true);
    }

    @Test
    void testUpdateConnectionCredentialsAllowsTheOwnerHoldingTheScope() throws Exception {
        assertScopeAndOwnerGuard(updateConnectionCredentialsMethod(), new Object[] {
            CONNECTION_ID, Map.of(), 1
        }, "CONNECTION_EDIT", true, true, false, true);
    }

    @Test
    void testUpdateConnectionCredentialsDeniesTheOwnerWithoutTheScope() throws Exception {
        assertScopeAndOwnerGuard(updateConnectionCredentialsMethod(), new Object[] {
            CONNECTION_ID, Map.of(), 1
        }, "CONNECTION_EDIT", false, true, false, false);
    }

    @Test
    void testUpdateConnectionCredentialsDeniesAScopeHolderWhoIsNotTheOwner() throws Exception {
        assertScopeAndOwnerGuard(updateConnectionCredentialsMethod(), new Object[] {
            CONNECTION_ID, Map.of(), 1
        }, "CONNECTION_EDIT", true, false, false, false);
    }

    @Test
    void testUpdateConnectionCredentialsHasNoTenantAdminAlternative() throws Exception {
        assertScopeAndOwnerGuard(updateConnectionCredentialsMethod(), new Object[] {
            CONNECTION_ID, Map.of(), 1
        }, "CONNECTION_EDIT", true, false, true, false);
    }

    @Test
    void testGetConnectionsDeniesWhenTheConnectionViewScopeIsRefusedInTheNamedEnvironment() throws Exception {
        assertWorkspaceGuard(false);
    }

    @Test
    void testGetConnectionsAllowsWhenTheConnectionViewScopeIsGrantedInTheNamedEnvironment() throws Exception {
        assertWorkspaceGuard(true);
    }

    @Test
    void testCreateDeniesWhenTheConnectionCreateScopeIsRefusedInTheConnectionsEnvironment() throws Exception {
        assertCreateGuard(false);
    }

    @Test
    void testCreateAllowsWhenTheConnectionCreateScopeIsGrantedInTheConnectionsEnvironment() throws Exception {
        assertCreateGuard(true);
    }

    @Test
    void testDisconnectConnectionIsTenantAdminOnly() throws Exception {
        // Disconnecting removes the connection from every workflow that uses it, across workspaces, so it stays with
        // the tenant administrator rather than any connection scope.
        Method method = WorkspaceConnectionFacadeImpl.class.getMethod("disconnectConnection", long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAuthority(\"ROLE_ADMIN\")");
    }

    private void assertResourceGuard(Method method, Object[] arguments, String expectedScope, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", CONNECTION_ID, CONNECTION_TYPE, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, expectedScope);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertScopeAndOwnerGuard(
        Method method, Object[] arguments, String scope, boolean scopeGranted, boolean owner, boolean admin,
        boolean expected) {

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(CONNECTION_ID, CONNECTION_TYPE, scope)).thenReturn(scopeGranted);
        when(permissionService.isResourceOwner(CONNECTION_TYPE, CONNECTION_ID)).thenReturn(owner);
        when(permissionService.isTenantAdmin()).thenReturn(admin);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s with scope %s=%s, owner=%s, tenantAdmin=%s", method.getName(), scope, scopeGranted, owner, admin)
            .isEqualTo(expected);
    }

    private void assertCreateGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "CONNECTION_CREATE", environment)).thenReturn(granted);

        Method method = WorkspaceConnectionFacadeImpl.class.getMethod("create", long.class, ConnectionDTO.class);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .environmentId((int) ENVIRONMENT_ID)
            .build();

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, connectionDTO
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "CONNECTION_CREATE", environment);
        verifyNoMoreInteractions(permissionService);
    }

    private void assertWorkspaceGuard(boolean granted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);
        Environment environment = Environment.values()[(int) ENVIRONMENT_ID];

        when(permissionService.hasWorkspaceScope(WORKSPACE_ID, "CONNECTION_VIEW", environment)).thenReturn(granted);

        Method method = WorkspaceConnectionFacadeImpl.class.getMethod(
            "getConnections", long.class, String.class, Integer.class, Long.class, Long.class);

        assertThat(evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, null, null, ENVIRONMENT_ID, null
        }))
            .isEqualTo(granted);

        verify(permissionService).hasWorkspaceScope(WORKSPACE_ID, "CONNECTION_VIEW", environment);
        verifyNoMoreInteractions(permissionService);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, Object[] arguments) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method deleteMethod() throws NoSuchMethodException {
        return WorkspaceConnectionFacadeImpl.class.getMethod("delete", long.class);
    }

    private static Method getConnectionMethod() throws NoSuchMethodException {
        return WorkspaceConnectionFacadeImpl.class.getMethod("getConnection", long.class);
    }

    private static Method updateMethod() throws NoSuchMethodException {
        return WorkspaceConnectionFacadeImpl.class.getMethod(
            "update", long.class, String.class, List.class, int.class);
    }

    private static Method updateConnectionCredentialsMethod() throws NoSuchMethodException {
        return WorkspaceConnectionFacadeImpl.class.getMethod(
            "updateConnectionCredentials", long.class, Map.class, int.class);
    }

    private static Method updateTagsMethod() throws NoSuchMethodException {
        return WorkspaceConnectionFacadeImpl.class.getMethod("updateTags", long.class, List.class);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link GitConfigurationFacadeImpl} through the real
 * {@link AutomationMethodSecurityExpressionHandler} backed by the real {@link AutomationPermissionEvaluator}, asserting
 * each guard in both directions and the exact check that reaches {@link PermissionService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class GitConfigurationFacadeAuthorizationTest {

    private static final long WORKSPACE_ID = 42L;

    @Test
    void testFetchGitConfigurationDeniesWhenTheWorkspaceManageScopeIsRefused() throws Exception {
        assertWorkspaceManageGuard(fetchGitConfigurationMethod(), new Object[] {
            WORKSPACE_ID
        }, false);
    }

    @Test
    void testFetchGitConfigurationAllowsWhenTheWorkspaceManageScopeIsGranted() throws Exception {
        assertWorkspaceManageGuard(fetchGitConfigurationMethod(), new Object[] {
            WORKSPACE_ID
        }, true);
    }

    @Test
    void testSaveDeniesWhenTheWorkspaceManageScopeIsRefused() throws Exception {
        assertWorkspaceManageGuard(saveMethod(), new Object[] {
            new GitConfigurationDTO("https://example.com/repository.git", "username", "password"), WORKSPACE_ID
        }, false);
    }

    @Test
    void testSaveAllowsWhenTheWorkspaceManageScopeIsGranted() throws Exception {
        assertWorkspaceManageGuard(saveMethod(), new Object[] {
            new GitConfigurationDTO("https://example.com/repository.git", "username", "password"), WORKSPACE_ID
        }, true);
    }

    @Test
    void testGetGitConfigurationCarriesNoScopeGuard() throws Exception {
        Method method = GitConfigurationFacadeImpl.class.getMethod("getGitConfiguration", long.class);

        assertThat(method.getAnnotation(PreAuthorize.class)).isNull();
    }

    private void assertWorkspaceManageGuard(Method method, Object[] arguments, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE")).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, 'Workspace', 'WORKSPACE_MANAGE') returns %s", method.getName(),
                granted ? "allow" : "deny", WORKSPACE_ID, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(WORKSPACE_ID, "Workspace", "WORKSPACE_MANAGE");
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

    private static Method fetchGitConfigurationMethod() throws NoSuchMethodException {
        return GitConfigurationFacadeImpl.class.getMethod("fetchGitConfiguration", long.class);
    }

    private static Method saveMethod() throws NoSuchMethodException {
        return GitConfigurationFacadeImpl.class.getMethod("save", GitConfigurationDTO.class, long.class);
    }
}

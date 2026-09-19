/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on {@link ProjectGitFacadeImpl}'s two mutating methods through a
 * real Spring expression handler backed by the real {@link AutomationPermissionEvaluator}.
 * <p>
 * Both guards are keyed on {@code 'Project'}, the token {@code ProjectOwnershipResolver} serves — naming a resource
 * type with no registered resolver would make {@code hasResourceScope} deny every non-tenant-admin, so each guard is
 * asserted in both directions rather than only on the denied path. {@code PROJECT_PULL} had no call site anywhere in
 * the tree before the pull guard, so this is also the first test that the catalogued scope is enforced by anything.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectGitFacadeAuthorizationTest {

    private static final long PROJECT_ID = 42L;
    private static final String RESOURCE_TYPE = "Project";

    @Test
    void testPullProjectFromGitDeniesWhenTheProjectPullScopeIsRefused() throws Exception {
        assertGuard(
            pullProjectFromGitMethod(), new Object[] {
                PROJECT_ID
            }, "PROJECT_PULL", false);
    }

    @Test
    void testPullProjectFromGitAllowsWhenTheProjectPullScopeIsGranted() throws Exception {
        assertGuard(
            pullProjectFromGitMethod(), new Object[] {
                PROJECT_ID
            }, "PROJECT_PULL", true);
    }

    @Test
    void testPushProjectToGitDeniesWhenTheProjectPushScopeIsRefused() throws Exception {
        assertGuard(
            pushProjectToGitMethod(), new Object[] {
                PROJECT_ID, "Update workflows"
            }, "PROJECT_PUSH", false);
    }

    @Test
    void testPushProjectToGitAllowsWhenTheProjectPushScopeIsGranted() throws Exception {
        assertGuard(
            pushProjectToGitMethod(), new Object[] {
                PROJECT_ID, "Update workflows"
            }, "PROJECT_PUSH", true);
    }

    private void assertGuard(Method method, Object[] arguments, String expectedScope, boolean granted) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(PROJECT_ID, RESOURCE_TYPE, expectedScope)).thenReturn(granted);

        assertThat(evaluateGuard(permissionService, method, arguments))
            .as("%s must %s when hasResourceScope(%s, '%s', '%s') returns %s", method.getName(),
                granted ? "allow" : "deny", PROJECT_ID, RESOURCE_TYPE, expectedScope, granted)
            .isEqualTo(granted);

        verify(permissionService).hasResourceScope(PROJECT_ID, RESOURCE_TYPE, expectedScope);
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode -- reading it rather than restating it as a literal is the entire point of the test, since a literal
    // could drift from the guard it claims to verify. It is not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, Object[] arguments) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(new Object(), method, arguments);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method pullProjectFromGitMethod() throws NoSuchMethodException {
        return ProjectGitFacadeImpl.class.getMethod("pullProjectFromGit", long.class);
    }

    private static Method pushProjectToGitMethod() throws NoSuchMethodException {
        return ProjectGitFacadeImpl.class.getMethod("pushProjectToGit", long.class, String.class);
    }
}

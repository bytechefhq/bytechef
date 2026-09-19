/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
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
 * Evaluates the real {@code @PreAuthorize} expression on {@link ProjectCodeWorkflowFacadeImpl#save}, the code workflow
 * deploy behind the public API, through a real Spring expression handler backed by the real
 * {@link AutomationPermissionEvaluator}. Deploying may create the project and always publishes it, so both scopes are
 * required in the target workspace before anything is written.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectCodeWorkflowFacadeAuthorizationTest {

    private static final String RESOURCE_TYPE = "Workspace";
    private static final long WORKSPACE_ID = 42L;

    @Test
    void testSaveAllowsWhenBothProjectCreateAndPublishScopesAreGranted() throws Exception {
        assertThat(evaluate(true, true)).isTrue();
    }

    @Test
    void testSaveDeniesWhenTheProjectCreateScopeIsRefused() throws Exception {
        assertThat(evaluate(false, true)).isFalse();
    }

    @Test
    void testSaveDeniesWhenTheProjectPublishScopeIsRefused() throws Exception {
        assertThat(evaluate(true, false)).isFalse();
    }

    private static boolean evaluate(boolean projectCreateGranted, boolean projectPublishGranted) throws Exception {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.hasResourceScope(WORKSPACE_ID, RESOURCE_TYPE, "PROJECT_CREATE"))
            .thenReturn(projectCreateGranted);
        when(permissionService.hasResourceScope(WORKSPACE_ID, RESOURCE_TYPE, "PROJECT_PUBLISH"))
            .thenReturn(projectPublishGranted);

        Method method = ProjectCodeWorkflowFacadeImpl.class.getMethod(
            "save", long.class, byte[].class, Language.class);

        return evaluateGuard(permissionService, method, new Object[] {
            WORKSPACE_ID, new byte[0], Language.JAVA
        });
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
}

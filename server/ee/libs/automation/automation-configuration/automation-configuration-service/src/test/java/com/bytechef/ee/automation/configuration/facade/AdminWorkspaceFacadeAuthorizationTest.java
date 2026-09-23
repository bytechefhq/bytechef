/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@link PreAuthorize} expressions on every {@link AdminWorkspaceFacadeImpl} method through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. The coarse
 * {@code ROLE_ADMIN} gate was moved off {@code WorkspaceApiController} onto this facade so it protects every caller; it
 * is load-bearing because {@code WorkspaceService.getWorkspaces()} is an intentionally unguarded trusted-caller method
 * and {@code WorkspaceService.getWorkspace(id)} only requires the {@code WORKSPACE_VIEW} scope. Each gate must decide
 * on the caller's {@code ROLE_ADMIN} authority alone and never consult {@link PermissionService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AdminWorkspaceFacadeAuthorizationTest {

    private static final List<String> METHOD_NAMES = List.of(
        "createWorkspace", "deleteWorkspace", "getWorkspace", "getWorkspaces", "updateWorkspace");

    @ParameterizedTest(name = "{0} admin={1}")
    @MethodSource("guardCases")
    void testGuardRequiresTheAdminAuthority(String methodName, boolean admin) {
        PermissionService permissionService = mock(PermissionService.class);

        assertThat(evaluateGuard(permissionService, findMethod(methodName), admin))
            .as("%s must %s a caller %s ROLE_ADMIN", methodName, admin ? "allow" : "deny",
                admin ? "holding" : "without")
            .isEqualTo(admin);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testEveryGuardedMethodHasAnEvaluatedCase() {
        Set<String> guardedMethodNames = Arrays.stream(AdminWorkspaceFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(PreAuthorize.class))
            .map(Method::getName)
            .collect(Collectors.toSet());

        assertThat(guardedMethodNames).containsExactlyInAnyOrderElementsOf(METHOD_NAMES);
    }

    static Stream<Arguments> guardCases() {
        return METHOD_NAMES.stream()
            .flatMap(methodName -> Stream.of(Arguments.of(methodName, false), Arguments.of(methodName, true)));
    }

    // The expression parsed here is read straight off our own @PreAuthorize annotation in this repository's compiled
    // bytecode, not attacker-influenced input.
    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method, boolean admin) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "alice", "credentials", List.of(new SimpleGrantedAuthority(admin ? "ROLE_ADMIN" : "ROLE_USER")));

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(AdminWorkspaceFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AdminWorkspaceFacadeImpl", methodName)
            .hasSize(1);

        return matches.getFirst();
    }
}

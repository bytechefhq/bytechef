/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.security.AutomationPermissionEvaluator;
import com.bytechef.automation.configuration.service.PermissionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expressions on the {@link ApiClientApiController} endpoints through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. API clients belong to
 * the tenant, not to a workspace, so each endpoint must decide on {@link PermissionService#isTenantAdmin()} alone.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiClientApiControllerTest {

    static Stream<Arguments> apiClientEndpoints() {
        return Stream.of(
            "createApiClient", "deleteApiClient", "getApiClient", "getApiClients", "updateApiClient")
            .flatMap(methodName -> Stream.of(Arguments.of(methodName, false), Arguments.of(methodName, true)));
    }

    @ParameterizedTest(name = "{0} tenantAdmin={1}")
    @MethodSource("apiClientEndpoints")
    void testApiClientEndpointRequiresATenantAdmin(String methodName, boolean tenantAdmin) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        assertThat(evaluateGuard(permissionService, findMethod(methodName)))
            .as("%s must %s when isTenantAdmin() returns %s", methodName, tenantAdmin ? "allow" : "deny", tenantAdmin)
            .isEqualTo(tenantAdmin);

        verify(permissionService).isTenantAdmin();
        verifyNoMoreInteractions(permissionService);
    }

    @SuppressFBWarnings(
        value = "SPEL_INJECTION",
        justification = "The expression is this repository's own @PreAuthorize value, not untrusted input.")
    private static boolean evaluateGuard(PermissionService permissionService, Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("%s must carry a @PreAuthorize guard", method.getName())
            .isNotNull();

        AutomationMethodSecurityExpressionHandler expressionHandler =
            new AutomationMethodSecurityExpressionHandler(permissionService);

        expressionHandler.setPermissionEvaluator(new AutomationPermissionEvaluator(permissionService));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", "credentials", List.of());

        SimpleMethodInvocation methodInvocation = new SimpleMethodInvocation(
            new Object(), method, new Object[method.getParameterCount()]);

        EvaluationContext evaluationContext =
            expressionHandler.createEvaluationContext(() -> authentication, methodInvocation);

        Expression expression = expressionHandler.getExpressionParser()
            .parseExpression(preAuthorize.value());

        return Boolean.TRUE.equals(expression.getValue(evaluationContext, Boolean.class));
    }

    private static Method findMethod(String methodName) {
        List<Method> methods = Arrays.stream(ApiClientApiController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()))
            .filter(method -> methodName.equals(method.getName()))
            .toList();

        assertThat(methods)
            .as("Expected exactly one public '%s' method on ApiClientApiController", methodName)
            .hasSize(1);

        return methods.getFirst();
    }
}

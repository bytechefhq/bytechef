/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on every query and mutation of
 * {@link McpIntegrationInstanceConfigurationGraphQlController} through the real
 * {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. These read and
 * administer the tenant's embedded MCP servers' integration configurations, so each must decide on
 * {@link PermissionService#isTenantAdmin()} alone.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceConfigurationGraphQlControllerTest {

    private static final List<Class<?>> CONTROLLER_CLASSES =
        List.of(McpIntegrationInstanceConfigurationGraphQlController.class);

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "createMcpIntegrationInstanceConfiguration", "deleteMcpIntegrationInstanceConfiguration",
        "mcpIntegrationInstanceConfiguration", "mcpIntegrationInstanceConfigurations",
        "mcpIntegrationInstanceConfigurationsByServerId", "updateMcpIntegrationInstanceConfiguration",
        "updateMcpIntegrationInstanceConfigurationVersion");

    static Stream<Arguments> endpoints() {
        return endpointMethods().flatMap(method -> Stream.of(Arguments.of(method, false), Arguments.of(method, true)));
    }

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = endpointMethods()
            .map(Method::getName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(ENDPOINT_NAMES));
    }

    @ParameterizedTest(name = "{0} tenantAdmin={1}")
    @MethodSource("endpoints")
    void testEndpointRequiresATenantAdmin(Method method, boolean tenantAdmin) {
        assertThat(Modifier.isPublic(method.getModifiers()))
            .as("%s must be public, since a proxy only enforces a public guard", method.getName())
            .isTrue();

        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        assertThat(evaluateGuard(permissionService, method))
            .as("%s must %s when isTenantAdmin() returns %s", method.getName(), tenantAdmin ? "allow" : "deny",
                tenantAdmin)
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

    private static Stream<Method> endpointMethods() {
        return CONTROLLER_CLASSES.stream()
            .flatMap(controllerClass -> Arrays.stream(controllerClass.getDeclaredMethods()))
            .filter(method -> method.isAnnotationPresent(MutationMapping.class) ||
                method.isAnnotationPresent(QueryMapping.class));
    }
}

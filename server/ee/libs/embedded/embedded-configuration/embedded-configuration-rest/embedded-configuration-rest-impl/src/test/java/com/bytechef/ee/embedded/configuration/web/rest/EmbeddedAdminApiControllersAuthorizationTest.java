/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.util.SimpleMethodInvocation;

/**
 * Evaluates the real {@code @PreAuthorize} expression on every endpoint of the embedded admin REST controllers through
 * the real {@link AutomationMethodSecurityExpressionHandler} and {@link AutomationPermissionEvaluator}. These endpoints
 * administer the tenant's embedded integrations, their instance configurations and app events, so each must decide on
 * {@link PermissionService#isTenantAdmin()} alone.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedAdminApiControllersAuthorizationTest {

    private static final List<Class<?>> CONTROLLER_CLASSES = List.of(
        AppEventApiController.class, CategoryApiController.class, IntegrationApiController.class,
        IntegrationInstanceApiController.class, IntegrationInstanceConfigurationApiController.class,
        IntegrationInstanceConfigurationTagApiController.class, IntegrationTagApiController.class);

    private static final Set<String> ENDPOINT_NAMES = Set.of(
        "AppEventApiController#createAppEvent", "AppEventApiController#deleteAppEvent",
        "AppEventApiController#getAppEvent", "AppEventApiController#getAppEvents",
        "AppEventApiController#updateAppEvent",
        "CategoryApiController#getIntegrationCategories",
        "IntegrationApiController#createIntegration", "IntegrationApiController#createIntegrationWorkflow",
        "IntegrationApiController#deleteIntegration", "IntegrationApiController#getIntegration",
        "IntegrationApiController#getIntegrationVersions", "IntegrationApiController#getIntegrations",
        "IntegrationApiController#publishIntegration", "IntegrationApiController#updateIntegration",
        "IntegrationInstanceApiController#deleteIntegrationInstance",
        "IntegrationInstanceApiController#enableIntegrationInstance",
        "IntegrationInstanceApiController#enableIntegrationInstanceWorkflow",
        "IntegrationInstanceApiController#getIntegrationInstance",
        "IntegrationInstanceConfigurationApiController#createIntegrationInstanceConfiguration",
        "IntegrationInstanceConfigurationApiController#createIntegrationInstanceConfigurationWorkflowJob",
        "IntegrationInstanceConfigurationApiController#deleteIntegrationInstanceConfiguration",
        "IntegrationInstanceConfigurationApiController#enableIntegrationInstanceConfiguration",
        "IntegrationInstanceConfigurationApiController#enableIntegrationInstanceConfigurationWorkflow",
        "IntegrationInstanceConfigurationApiController#getIntegrationInstanceConfiguration",
        "IntegrationInstanceConfigurationApiController#getIntegrationInstanceConfigurations",
        "IntegrationInstanceConfigurationApiController#updateIntegrationInstanceConfiguration",
        "IntegrationInstanceConfigurationApiController#updateIntegrationInstanceConfigurationWorkflow",
        "IntegrationInstanceConfigurationTagApiController#getIntegrationInstanceConfigurationTags",
        "IntegrationInstanceConfigurationTagApiController#updateIntegrationInstanceConfigurationTags",
        "IntegrationTagApiController#getIntegrationTags", "IntegrationTagApiController#updateIntegrationTags");

    static Stream<Arguments> endpoints() {
        return endpointMethods().flatMap(
            method -> Stream.of(Arguments.of(endpointName(method), method, false),
                Arguments.of(endpointName(method), method, true)));
    }

    @Test
    void testEveryEndpointIsEvaluated() {
        Set<String> endpointNames = endpointMethods()
            .map(EmbeddedAdminApiControllersAuthorizationTest::endpointName)
            .collect(Collectors.toCollection(TreeSet::new));

        assertThat(endpointNames).isEqualTo(new TreeSet<>(ENDPOINT_NAMES));
    }

    @ParameterizedTest(name = "{0} tenantAdmin={2}")
    @MethodSource("endpoints")
    void testEndpointRequiresATenantAdmin(String endpointName, Method method, boolean tenantAdmin) {
        PermissionService permissionService = mock(PermissionService.class);

        when(permissionService.isTenantAdmin()).thenReturn(tenantAdmin);

        assertThat(evaluateGuard(permissionService, method))
            .as("%s must %s when isTenantAdmin() returns %s", endpointName, tenantAdmin ? "allow" : "deny",
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
            .as("%s must carry a @PreAuthorize guard", endpointName(method))
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

    private static String endpointName(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();

        return declaringClass.getSimpleName() + "#" + method.getName();
    }

    private static Stream<Method> endpointMethods() {
        return CONTROLLER_CLASSES.stream()
            .flatMap(controllerClass -> Arrays.stream(controllerClass.getDeclaredMethods()))
            .filter(method -> !method.isSynthetic() && Modifier.isPublic(method.getModifiers()));
    }
}

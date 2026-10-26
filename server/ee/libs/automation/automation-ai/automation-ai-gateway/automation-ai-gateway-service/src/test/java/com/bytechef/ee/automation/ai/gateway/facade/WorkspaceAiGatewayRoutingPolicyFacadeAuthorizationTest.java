/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceAiGatewayRoutingPolicyFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testCreateWorkspaceRoutingPolicyRequiresAdmin() {
        assertExpression("createWorkspaceRoutingPolicy", ADMIN_EXPRESSION);
    }

    @Test
    void testDeleteWorkspaceRoutingPolicyRequiresAdmin() {
        assertExpression("deleteWorkspaceRoutingPolicy", ADMIN_EXPRESSION);
    }

    @Test
    void testGetWorkspaceRoutingPoliciesRequiresViewer() {
        assertExpression("getWorkspaceRoutingPolicies", VIEWER_EXPRESSION);
    }

    @Test
    void testUpdateWorkspaceRoutingPolicyRequiresAdmin() {
        assertExpression("updateWorkspaceRoutingPolicy", ADMIN_EXPRESSION);
    }

    @Test
    void testEveryFacadeMethodRequiresPreAuthorize() {
        List<Method> interfaceMethods = Arrays.asList(WorkspaceAiGatewayRoutingPolicyFacade.class.getMethods());

        assertThat(interfaceMethods)
            .as(
                "Sanity check: WorkspaceAiGatewayRoutingPolicyFacade must declare at least one method, or this sweep "
                    + "is vacuous")
            .isNotEmpty();

        for (Method interfaceMethod : interfaceMethods) {
            Method implMethod = resolveImplMethod(interfaceMethod);

            assertThat(implMethod.getAnnotation(PreAuthorize.class))
                .as(
                    "Method '%s' on WorkspaceAiGatewayRoutingPolicyFacadeImpl must have @PreAuthorize -- a newly "
                        + "added facade method with no annotation would otherwise pass every other test in this class",
                    interfaceMethod.getName())
                .isNotNull();
        }
    }

    private static Method resolveImplMethod(Method interfaceMethod) {
        try {
            return WorkspaceAiGatewayRoutingPolicyFacadeImpl.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(
                "WorkspaceAiGatewayRoutingPolicyFacadeImpl does not implement " + interfaceMethod, exception);
        }
    }

    private static void assertExpression(String methodName, String expression) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must have @PreAuthorize", methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' @PreAuthorize expression", methodName)
            .isEqualTo(expression);
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(WorkspaceAiGatewayRoutingPolicyFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as(
                "Expected exactly one non-synthetic '%s' method on WorkspaceAiGatewayRoutingPolicyFacadeImpl",
                methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

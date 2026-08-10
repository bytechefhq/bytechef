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
 * Pins the {@link PreAuthorize} annotations on {@link WorkspaceAiGatewayProviderFacadeImpl}. Reads require the
 * caller-bound {@code AI_GATEWAY_VIEW} workspace permission; writes require the {@code ADMIN} authority. Plain unit
 * tests run without Spring's method-security AOP and {@link WorkspaceAiGatewayProviderFacadeTest} constructs the impl
 * directly, so neither exercises method security — this reflection test catches a refactor that silently drops or
 * weakens a guard.
 *
 * <p>
 * The per-method tests below catch a <em>changed</em> expression on a method they name, but pass vacuously for a newly
 * added facade method that carries no annotation at all -- {@link #testEveryFacadeMethodRequiresPreAuthorize()} closes
 * that gap by sweeping every method declared on {@link WorkspaceAiGatewayProviderFacade} and asserting the impl
 * override carries {@code @PreAuthorize}, regardless of whether this class also names it.
 *
 * <p>
 * Runtime enforcement of these expressions by Spring Security is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceAiGatewayProviderFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testCreateWorkspaceProviderRequiresAdmin() {
        assertExpression("createWorkspaceProvider", ADMIN_EXPRESSION);
    }

    @Test
    void testDeleteWorkspaceProviderRequiresAdmin() {
        assertExpression("deleteWorkspaceProvider", ADMIN_EXPRESSION);
    }

    @Test
    void testGetWorkspaceProvidersRequiresViewer() {
        assertExpression("getWorkspaceProviders", VIEWER_EXPRESSION);
    }

    @Test
    void testTestWorkspaceProviderConnectionRequiresAdmin() {
        assertExpression("testWorkspaceProviderConnection", ADMIN_EXPRESSION);
    }

    @Test
    void testUpdateWorkspaceProviderRequiresAdmin() {
        assertExpression("updateWorkspaceProvider", ADMIN_EXPRESSION);
    }

    @Test
    void testEveryFacadeMethodRequiresPreAuthorize() {
        List<Method> interfaceMethods = Arrays.asList(WorkspaceAiGatewayProviderFacade.class.getMethods());

        assertThat(interfaceMethods)
            .as(
                "Sanity check: WorkspaceAiGatewayProviderFacade must declare at least one method, or this sweep is "
                    + "vacuous")
            .isNotEmpty();

        for (Method interfaceMethod : interfaceMethods) {
            Method implMethod = resolveImplMethod(interfaceMethod);

            assertThat(implMethod.getAnnotation(PreAuthorize.class))
                .as(
                    "Method '%s' on WorkspaceAiGatewayProviderFacadeImpl must have @PreAuthorize -- a newly added "
                        + "facade method with no annotation would otherwise pass every other test in this class",
                    interfaceMethod.getName())
                .isNotNull();
        }
    }

    private static Method resolveImplMethod(Method interfaceMethod) {
        try {
            return WorkspaceAiGatewayProviderFacadeImpl.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(
                "WorkspaceAiGatewayProviderFacadeImpl does not implement " + interfaceMethod, exception);
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
        List<Method> matches = Arrays.stream(WorkspaceAiGatewayProviderFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on WorkspaceAiGatewayProviderFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

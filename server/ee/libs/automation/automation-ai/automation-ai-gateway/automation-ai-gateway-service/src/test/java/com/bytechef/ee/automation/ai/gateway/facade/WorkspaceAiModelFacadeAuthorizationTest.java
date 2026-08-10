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
 * Pins the {@link PreAuthorize} annotations on {@link WorkspaceAiModelFacadeImpl}. Reads require the caller-bound
 * {@code AI_GATEWAY_VIEW} workspace permission; writes require the {@code ADMIN} authority. Plain unit tests run
 * without Spring's method-security AOP and {@link WorkspaceAiModelFacadeTest} constructs the impl directly, so neither
 * exercises method security — this reflection test catches a refactor that silently drops or weakens a guard.
 *
 * <p>
 * The per-method tests below catch a <em>changed</em> expression on a method they name, but pass vacuously for a newly
 * added facade method that carries no annotation at all -- {@link #testEveryFacadeMethodRequiresPreAuthorize()} closes
 * that gap by sweeping every method declared on {@link WorkspaceAiModelFacade} and asserting the impl override carries
 * {@code @PreAuthorize}, regardless of whether this class also names it.
 *
 * <p>
 * Runtime enforcement of these expressions by Spring Security is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceAiModelFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testCreateWorkspaceModelRequiresAdmin() {
        assertExpression("createWorkspaceModel", ADMIN_EXPRESSION);
    }

    @Test
    void testDeleteWorkspaceModelRequiresAdmin() {
        assertExpression("deleteWorkspaceModel", ADMIN_EXPRESSION);
    }

    @Test
    void testGetWorkspaceModelsRequiresViewer() {
        assertExpression("getWorkspaceModels", VIEWER_EXPRESSION);
    }

    @Test
    void testUnpinWorkspaceModelRequiresAdmin() {
        assertExpression("unpinWorkspaceModel", ADMIN_EXPRESSION);
    }

    @Test
    void testUpdateWorkspaceModelRequiresAdmin() {
        assertExpression("updateWorkspaceModel", ADMIN_EXPRESSION);
    }

    @Test
    void testEveryFacadeMethodRequiresPreAuthorize() {
        List<Method> interfaceMethods = Arrays.asList(WorkspaceAiModelFacade.class.getMethods());

        assertThat(interfaceMethods)
            .as("Sanity check: WorkspaceAiModelFacade must declare at least one method, or this sweep is vacuous")
            .isNotEmpty();

        for (Method interfaceMethod : interfaceMethods) {
            Method implMethod = resolveImplMethod(interfaceMethod);

            assertThat(implMethod.getAnnotation(PreAuthorize.class))
                .as(
                    "Method '%s' on WorkspaceAiModelFacadeImpl must have @PreAuthorize -- a newly added facade "
                        + "method with no annotation would otherwise pass every other test in this class",
                    interfaceMethod.getName())
                .isNotNull();
        }
    }

    private static Method resolveImplMethod(Method interfaceMethod) {
        try {
            return WorkspaceAiModelFacadeImpl.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(
                "WorkspaceAiModelFacadeImpl does not implement " + interfaceMethod, exception);
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
        List<Method> matches = Arrays.stream(WorkspaceAiModelFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on WorkspaceAiModelFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

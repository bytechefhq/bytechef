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
 * Pins the {@link PreAuthorize} annotations on {@link AiModelFacadeImpl}, following the
 * {@link AiEvalRuleFacadeAuthorizationTest} pattern for this module. {@code reconcileCatalog} and {@code unpin} are the
 * two methods this test was added for: an unguarded {@code reconcileCatalog} would let any authenticated caller trigger
 * a full-table catalog write, and an unguarded {@code unpin} would let any caller strip an administrator's override off
 * a row. Plain unit tests run without Spring's method-security AOP, so the annotation presence must be asserted
 * reflectively; runtime enforcement of these expressions by Spring Security is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * <p>
 * The per-method tests above catch a <em>changed</em> expression on a method they name, but pass vacuously for a newly
 * added facade method that carries no annotation at all -- {@link #testEveryFacadeMethodRequiresPreAuthorize()} closes
 * that gap by sweeping every method declared on {@link AiModelFacade} and asserting the impl override carries
 * {@code @PreAuthorize}, regardless of whether this class also names it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiModelFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testGetModelRequiresAdmin() {
        assertExpression("getModel");
    }

    @Test
    void testGetModelsRequiresAdmin() {
        assertExpression("getModels");
    }

    @Test
    void testGetModelsByProviderIdRequiresAdmin() {
        assertExpression("getModelsByProviderId");
    }

    @Test
    void testCreateRequiresAdmin() {
        assertExpression("create");
    }

    @Test
    void testDeleteRequiresAdmin() {
        assertExpression("delete");
    }

    @Test
    void testUpdateRequiresAdmin() {
        assertExpression("update");
    }

    @Test
    void testReconcileCatalogRequiresAdmin() {
        assertExpression("reconcileCatalog");
    }

    @Test
    void testUnpinRequiresAdmin() {
        assertExpression("unpin");
    }

    @Test
    void testEveryFacadeMethodRequiresPreAuthorize() {
        List<Method> interfaceMethods = Arrays.asList(AiModelFacade.class.getMethods());

        assertThat(interfaceMethods)
            .as("Sanity check: AiModelFacade must declare at least one method, or this sweep is vacuous")
            .isNotEmpty();

        for (Method interfaceMethod : interfaceMethods) {
            Method implMethod = resolveImplMethod(interfaceMethod);

            assertThat(implMethod.getAnnotation(PreAuthorize.class))
                .as(
                    "Method '%s' on AiModelFacadeImpl must have @PreAuthorize -- a newly added facade method "
                        + "with no annotation would otherwise pass every other test in this class",
                    interfaceMethod.getName())
                .isNotNull();
        }
    }

    private static Method resolveImplMethod(Method interfaceMethod) {
        try {
            return AiModelFacadeImpl.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("AiModelFacadeImpl does not implement " + interfaceMethod, exception);
        }
    }

    private static void assertExpression(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must have @PreAuthorize", methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' @PreAuthorize expression", methodName)
            .isEqualTo(ADMIN_EXPRESSION);
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(AiModelFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AiModelFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.ai.eval.facade.AiEvalRuleFacade;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link AiEvalRuleFacadeImpl}. The guards were moved off
 * {@code AiEvalRuleGraphQlController} onto the facade so they protect every caller of the facade, not just the GraphQL
 * entry point; this reflection test catches a refactor that silently drops or weakens one. Plain unit tests run without
 * Spring's method-security AOP, so the annotation presence must be asserted reflectively.
 *
 * <p>
 * The per-method tests below catch a <em>changed</em> expression on a method they name, but pass vacuously for a newly
 * added facade method that carries no annotation at all -- {@link #testEveryFacadeMethodRequiresPreAuthorize()} closes
 * that gap by sweeping every method declared on {@link AiEvalRuleFacade} and asserting the impl override carries
 * {@code @PreAuthorize}, regardless of whether this class also names it. That sweep is what caught
 * {@code listTemplates} previously shipping with no annotation at all.
 *
 * <p>
 * Runtime enforcement of these expressions by Spring Security is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiEvalRuleFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String AUTHENTICATED_EXPRESSION = "isAuthenticated()";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testGetEvalRuleRequiresAdmin() {
        assertExpression("getEvalRule", ADMIN_EXPRESSION);
    }

    @Test
    void testGetEvalRulesByWorkspaceRequiresViewer() {
        assertExpression("getEvalRulesByWorkspace", VIEWER_EXPRESSION);
    }

    @Test
    void testGetExecutionsByEvalRuleRequiresAdmin() {
        assertExpression("getExecutionsByEvalRule", ADMIN_EXPRESSION);
    }

    @Test
    void testGetExecutionsByTraceRequiresAdmin() {
        assertExpression("getExecutionsByTrace", ADMIN_EXPRESSION);
    }

    @Test
    void testCreateInWorkspaceRequiresAdmin() {
        assertExpression("createInWorkspace", ADMIN_EXPRESSION);
    }

    @Test
    void testDeleteInWorkspaceRequiresAdmin() {
        assertExpression("deleteInWorkspace", ADMIN_EXPRESSION);
    }

    @Test
    void testRunOnHistoricalTracesRequiresAdmin() {
        assertExpression("runOnHistoricalTraces", ADMIN_EXPRESSION);
    }

    @Test
    void testUpdateRequiresAdmin() {
        assertExpression("update", ADMIN_EXPRESSION);
    }

    @Test
    void testInstantiateTemplateRequiresAdmin() {
        assertExpression("instantiateTemplate", ADMIN_EXPRESSION);
    }

    @Test
    void testListTemplatesRequiresAuthentication() {
        assertExpression("listTemplates", AUTHENTICATED_EXPRESSION);
    }

    @Test
    void testEveryFacadeMethodRequiresPreAuthorize() {
        List<Method> interfaceMethods = Arrays.asList(AiEvalRuleFacade.class.getMethods());

        assertThat(interfaceMethods)
            .as("Sanity check: AiEvalRuleFacade must declare at least one method, or this sweep is vacuous")
            .isNotEmpty();

        for (Method interfaceMethod : interfaceMethods) {
            Method implMethod = resolveImplMethod(interfaceMethod);

            assertThat(implMethod.getAnnotation(PreAuthorize.class))
                .as(
                    "Method '%s' on AiEvalRuleFacadeImpl must have @PreAuthorize -- a newly added facade method with "
                        + "no annotation would otherwise pass every other test in this class",
                    interfaceMethod.getName())
                .isNotNull();
        }
    }

    private static Method resolveImplMethod(Method interfaceMethod) {
        try {
            return AiEvalRuleFacadeImpl.class.getMethod(
                interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("AiEvalRuleFacadeImpl does not implement " + interfaceMethod, exception);
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
        List<Method> matches = Arrays.stream(AiEvalRuleFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AiEvalRuleFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

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
 * Pins the {@link PreAuthorize} annotations on {@link AiEvalRuleFacadeImpl}. The guards were moved off
 * {@code AiEvalRuleGraphQlController} onto the facade so they protect every caller of the facade, not just the GraphQL
 * entry point; this reflection test catches a refactor that silently drops or weakens one. Plain unit tests run without
 * Spring's method-security AOP, so the annotation presence must be asserted reflectively.
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

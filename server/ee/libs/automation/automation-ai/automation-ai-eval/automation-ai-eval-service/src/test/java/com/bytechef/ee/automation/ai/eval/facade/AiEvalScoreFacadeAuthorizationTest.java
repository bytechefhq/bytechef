/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link AiEvalScoreFacadeImpl}. The guards were moved off
 * {@code AiEvalScoreGraphQlController} onto the facade so they protect every caller of the facade, not just the GraphQL
 * entry point; this reflection test catches a refactor that silently drops or weakens one. Note the two distinct guards
 * on the two workspace-scoped reads: {@code getScoresByWorkspace} is VIEWER (the operator list view) while
 * {@code getScoresByWorkspaceForAnalytics} is ADMIN (the admin analytics view).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiEvalScoreFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testCreateInWorkspaceRequiresAdmin() {
        assertExpression("createInWorkspace", ADMIN_EXPRESSION);
    }

    @Test
    void testDeleteInWorkspaceRequiresAdmin() {
        assertExpression("deleteInWorkspace", ADMIN_EXPRESSION);
    }

    @Test
    void testGetScoresByWorkspaceRequiresViewer() {
        assertExpression("getScoresByWorkspace", VIEWER_EXPRESSION);
    }

    @Test
    void testGetScoresByWorkspaceForAnalyticsRequiresAdmin() {
        assertExpression("getScoresByWorkspaceForAnalytics", ADMIN_EXPRESSION);
    }

    @Test
    void testGetScoresByTraceRequiresAdmin() {
        assertExpression("getScoresByTrace", ADMIN_EXPRESSION);
    }

    @Test
    void testGetScoreTrendRequiresAdmin() {
        assertExpression("getScoreTrend", ADMIN_EXPRESSION);
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
        List<Method> matches = Arrays.stream(AiEvalScoreFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AiEvalScoreFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

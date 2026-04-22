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
 * Pins the {@link PreAuthorize} annotations on {@link AiEvalScoreConfigFacadeImpl}. The guards were moved off
 * {@code AiEvalScoreConfigGraphQlController} onto the facade so they protect every caller of the facade, not just the
 * GraphQL entry point; this reflection test catches a refactor that silently drops or weakens one.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiEvalScoreConfigFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String VIEWER_EXPRESSION = "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testGetScoreConfigRequiresAdmin() {
        assertExpression("getScoreConfig", ADMIN_EXPRESSION);
    }

    @Test
    void testGetScoreConfigsByWorkspaceRequiresViewer() {
        assertExpression("getScoreConfigsByWorkspace", VIEWER_EXPRESSION);
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
    void testUpdateRequiresAdmin() {
        assertExpression("update", ADMIN_EXPRESSION);
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
        List<Method> matches = Arrays.stream(AiEvalScoreConfigFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AiEvalScoreConfigFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

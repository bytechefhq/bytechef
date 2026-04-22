/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} annotations on {@link AiPromptFacadeImpl}. The guards were moved off
 * {@code AiPromptGraphQlController} onto the facade so they protect every caller of the facade (not just the GraphQL
 * entry point) and stay off the shared platform/workspace services the gateway playground and observability export rely
 * on; this reflection test catches a refactor that silently drops one or weakens an expression.
 *
 * <p>
 * Runtime enforcement of these expressions by Spring Security is proven generically by
 * {@code PreAuthorizeProxyEnforcementIntTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiPromptFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";
    private static final String WORKSPACE_VIEWER_EXPRESSION =
        "hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')";

    @Test
    void testGetPromptRequiresAdmin() {
        assertExpression("getPrompt", ADMIN_EXPRESSION);
    }

    @Test
    void testGetPromptsByWorkspaceRequiresWorkspaceViewer() {
        assertExpression("getPromptsByWorkspace", WORKSPACE_VIEWER_EXPRESSION);
    }

    @Test
    void testGetVersionsByPromptRequiresAdmin() {
        assertExpression("getVersionsByPrompt", ADMIN_EXPRESSION);
    }

    @Test
    void testCreateInWorkspaceRequiresAdmin() {
        assertExpression("createInWorkspace", ADMIN_EXPRESSION);
    }

    @Test
    void testGetNextVersionNumberRequiresAdmin() {
        assertExpression("getNextVersionNumber", ADMIN_EXPRESSION);
    }

    @Test
    void testCreateVersionRequiresAdmin() {
        assertExpression("createVersion", ADMIN_EXPRESSION);
    }

    @Test
    void testDeleteInWorkspaceRequiresAdmin() {
        assertExpression("deleteInWorkspace", ADMIN_EXPRESSION);
    }

    @Test
    void testSetActiveVersionRequiresAdmin() {
        assertExpression("setActiveVersion", ADMIN_EXPRESSION);
    }

    @Test
    void testUpdateRequiresAdmin() {
        assertExpression("update", ADMIN_EXPRESSION);
    }

    private static void assertExpression(String methodName, String expression) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as(
                "Method '%s' must have @PreAuthorize; dropping it would silently let every authenticated user perform "
                    + "a guarded operation.",
                methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' @PreAuthorize expression must equal '%s'", methodName, expression)
            .isEqualTo(expression);
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(AiPromptFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AiPromptFacadeImpl", methodName)
            .hasSize(1);

        return matches.get(0);
    }
}

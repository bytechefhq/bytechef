/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

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
class AdminWorkspaceFacadeAuthorizationTest {

    private static final String ADMIN_EXPRESSION = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testEveryMutationAndReadRequiresAdmin() {
        for (String methodName : List.of(
            "createWorkspace", "deleteWorkspace", "getWorkspace", "getWorkspaces", "updateWorkspace")) {

            PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize)
                .as(
                    "Method '%s' must have @PreAuthorize(hasAuthority(\"ROLE_ADMIN\")); dropping it would silently "
                        + "expose the workspace admin REST surface (getWorkspaces is unguarded at the service).",
                    methodName)
                .isNotNull();

            assertThat(preAuthorize.value())
                .as("Method '%s' @PreAuthorize expression must require ROLE_ADMIN", methodName)
                .isEqualTo(ADMIN_EXPRESSION);
        }
    }

    private static Method findMethod(String methodName) {
        List<Method> matches = Arrays.stream(AdminWorkspaceFacadeImpl.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> method.getName()
                .equals(methodName))
            .toList();

        assertThat(matches)
            .as("Expected exactly one non-synthetic '%s' method on AdminWorkspaceFacadeImpl", methodName)
            .hasSize(1);

        return matches.getFirst();
    }
}

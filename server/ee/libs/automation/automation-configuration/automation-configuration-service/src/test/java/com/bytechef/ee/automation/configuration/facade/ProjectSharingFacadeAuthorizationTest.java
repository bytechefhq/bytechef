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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@link PreAuthorize} expressions on {@link ProjectSharingFacadeImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectSharingFacadeAuthorizationTest {

    private static final String OWNER_OR_ADMIN_EXPRESSION =
        "@permissionService.isResourceOwner('Project', #projectId) || " +
            "@permissionService.hasResourceRole(#projectId, 'Project', 'ADMIN')";

    @ParameterizedTest
    @ValueSource(strings = {
        "setProjectVisibility", "grantProjectAccess", "revokeProjectAccess", "getProjectGrants"
    })
    void testSharingMethodsRequireOwnerOrAdmin(String methodName) {
        PreAuthorize preAuthorize = findMethod(methodName).getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("Method '%s' must carry @PreAuthorize", methodName)
            .isNotNull();

        assertThat(preAuthorize.value())
            .as("Method '%s' must be owner-or-admin", methodName)
            .isEqualTo(OWNER_OR_ADMIN_EXPRESSION);
    }

    @Test
    void testEverySharingMethodIsCovered() {
        List<String> annotatedMethods = Arrays.stream(ProjectSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getAnnotation(PreAuthorize.class) != null)
            .map(Method::getName)
            .distinct()
            .sorted()
            .toList();

        assertThat(annotatedMethods)
            .containsExactly("getProjectGrants", "grantProjectAccess", "revokeProjectAccess", "setProjectVisibility");
    }

    private static Method findMethod(String methodName) {
        return Arrays.stream(ProjectSharingFacadeImpl.class.getDeclaredMethods())
            .filter(method -> method.getName()
                .equals(methodName))
            .filter(method -> !method.isSynthetic())
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected '" + methodName + "' on ProjectSharingFacadeImpl"));
    }
}

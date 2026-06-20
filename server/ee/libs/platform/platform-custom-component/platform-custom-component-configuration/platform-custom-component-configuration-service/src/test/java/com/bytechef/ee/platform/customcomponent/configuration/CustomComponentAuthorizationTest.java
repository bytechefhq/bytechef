/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacadeImpl;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentServiceImpl;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close custom-component IDOR (T25). Custom components are
 * tenant-global uploaded code, so management (upload/delete/enable) and the admin-UI reads require the platform admin
 * authority, matching the pre-existing gate on {@code save}. The runtime registry loads components via the service's
 * {@code getCustomComponents()} (plural), which stays ungated -- a negative assertion locks that in.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentAuthorizationTest {

    private static final String ADMIN = "hasAuthority(\"ROLE_ADMIN\")";

    @Test
    void testFacadeDeleteRequiresAdmin() {
        assertExpression(CustomComponentFacadeImpl.class, "delete", ADMIN);
    }

    @Test
    void testFacadeSaveRequiresAdmin() {
        assertExpression(CustomComponentFacadeImpl.class, "save", ADMIN);
    }

    @Test
    void testFacadeGetCustomComponentsRequiresAdmin() {
        assertExpression(CustomComponentFacadeImpl.class, "getCustomComponents", ADMIN);
    }

    @Test
    void testFacadeGetCustomComponentDefinitionRequiresAdmin() {
        assertExpression(CustomComponentFacadeImpl.class, "getCustomComponentDefinition", ADMIN);
    }

    @Test
    void testServiceEnableCustomComponentRequiresAdmin() {
        assertExpression(CustomComponentServiceImpl.class, "enableCustomComponent", ADMIN);
    }

    @Test
    void testServiceGetCustomComponentRequiresAdmin() {
        assertExpression(CustomComponentServiceImpl.class, "getCustomComponent", ADMIN);
    }

    @Test
    void testServiceGetCustomComponentsIsNotGated() {
        Method match = null;

        for (Method candidate : CustomComponentServiceImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals("getCustomComponents")) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("getCustomComponents method")
            .isNotNull();
        assertThat(match.isAnnotationPresent(PreAuthorize.class))
            .as("runtime-shared getCustomComponents() must NOT carry @PreAuthorize")
            .isFalse();
    }

    private static void assertExpression(Class<?> clazz, String methodName, String expression) {
        Method match = null;

        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method %s on %s", methodName, clazz.getSimpleName())
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo(expression);
    }
}

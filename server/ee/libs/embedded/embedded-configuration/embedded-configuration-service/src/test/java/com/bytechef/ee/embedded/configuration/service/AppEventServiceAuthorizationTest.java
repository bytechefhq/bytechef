/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close app-event IDOR (T24). App events are tenant-level embedded
 * configuration, so the management writes require tenant admin. The reads ({@code getAppEvent}/{@code getAppEvents})
 * are invoked by {@code AppEventTrigger} during workflow execution (no {@code SecurityContext}) and stay ungated --
 * negative assertions lock that in.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppEventServiceAuthorizationTest {

    @Test
    void testCreateRequiresTenantAdmin() {
        assertAdmin("create");
    }

    @Test
    void testDeleteRequiresTenantAdmin() {
        assertAdmin("delete");
    }

    @Test
    void testUpdateRequiresTenantAdmin() {
        assertAdmin("update");
    }

    @Test
    void testGetAppEventIsNotGated() {
        assertNotGated("getAppEvent");
    }

    @Test
    void testGetAppEventsIsNotGated() {
        assertNotGated("getAppEvents");
    }

    private static void assertAdmin(String methodName) {
        Method match = findMethod(methodName);

        PreAuthorize preAuthorize = match.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission('Tenant', 'ADMIN')");
    }

    private static void assertNotGated(String methodName) {
        Method match = findMethod(methodName);

        assertThat(match.isAnnotationPresent(PreAuthorize.class))
            .as("runtime-shared read %s must NOT carry @PreAuthorize", methodName)
            .isFalse();
    }

    private static Method findMethod(String methodName) {
        for (Method candidate : AppEventServiceImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {

                return candidate;
            }
        }

        throw new AssertionError("method " + methodName + " not found");
    }
}

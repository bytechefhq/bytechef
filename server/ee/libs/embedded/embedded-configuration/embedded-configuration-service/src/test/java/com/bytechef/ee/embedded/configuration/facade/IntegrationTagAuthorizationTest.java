/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close embedded integration/instance-configuration tag IDOR (T25).
 * These are tenant-level embedded admin configuration tags (the {@code /internal} dashboard), with no runtime callers,
 * so list and update require tenant admin.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationTagAuthorizationTest {

    @Test
    void testGetIntegrationTagsRequiresTenantAdmin() {
        assertAdmin(IntegrationTagFacadeImpl.class, "getIntegrationTags");
    }

    @Test
    void testUpdateIntegrationTagsRequiresTenantAdmin() {
        assertAdmin(IntegrationTagFacadeImpl.class, "updateIntegrationTags", long.class, List.class);
    }

    @Test
    void testGetIntegrationInstanceConfigurationTagsRequiresTenantAdmin() {
        assertAdmin(IntegrationInstanceConfigurationFacadeImpl.class, "getIntegrationInstanceConfigurationTags");
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationTagsRequiresTenantAdmin() {
        assertAdmin(
            IntegrationInstanceConfigurationFacadeImpl.class, "updateIntegrationInstanceConfigurationTags",
            long.class, List.class);
    }

    private static void assertAdmin(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Method method;

        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasPermission('Tenant', 'ADMIN')");
    }
}

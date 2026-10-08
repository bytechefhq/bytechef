/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PreAuthorizeAnnotationTest {

    @Test
    void testServiceImplementationsCarryNoAuthorizationChecks() throws Exception {
        for (String className : new String[] {
            "com.bytechef.automation.configuration.service.ProjectDeploymentServiceImpl",
            "com.bytechef.automation.configuration.service.ProjectServiceImpl",
            "com.bytechef.automation.configuration.service.ProjectWorkflowServiceImpl",
            "com.bytechef.ee.automation.configuration.service.WorkspaceUserServiceImpl"
        }) {
            Class<?> clazz = Class.forName(className);

            for (Method method : clazz.getDeclaredMethods()) {
                assertThat(method.isAnnotationPresent(PreAuthorize.class))
                    .as(clazz.getSimpleName() + "." + method.getName() + " must not carry @PreAuthorize")
                    .isFalse();
            }
        }
    }
}

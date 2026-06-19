/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that owner-isolate API-client operations (T19).
 *
 * @author Ivica Cardic
 * @version ee
 */
class ApiClientServiceAuthorizationTest {

    @Test
    void testGetApiClientRequiresOwner() {
        assertExpression("getApiClient", "hasPermission(#id, 'ApiClient:ResourceOwner', 'SELF')");
    }

    @Test
    void testUpdateRequiresOwner() {
        assertExpression("update", "hasPermission(#apiClient.id, 'ApiClient:ResourceOwner', 'SELF')");
    }

    @Test
    void testDeleteRequiresOwner() {
        assertExpression("delete", "hasPermission(#id, 'ApiClient:ResourceOwner', 'SELF')");
    }

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : ApiClientServiceImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}

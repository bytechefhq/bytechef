/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close personal API-key IDOR (T19), enforced at the facade tier (moved
 * off {@code ApiKeyGraphQlController}). Per-id ops are owner-isolated via {@code ApiKey:ResourceOwner};
 * {@code getAdminApiKeys} is tenant-admin only; create/list require authentication.
 *
 * @author Ivica Cardic
 */
class ApiKeyFacadeAuthorizationTest {

    @Test
    void testGetApiKeyRequiresOwner() {
        assertExpression("getApiKey", "isResourceOwner(#id, 'ApiKey')");
    }

    @Test
    void testDeleteRequiresOwner() {
        assertExpression("delete", "isResourceOwner(#id, 'ApiKey')");
    }

    @Test
    void testUpdateRequiresOwner() {
        assertExpression("update", "isResourceOwner(#apiKey.id, 'ApiKey')");
    }

    @Test
    void testGetAdminApiKeysRequiresTenantAdmin() {
        assertExpression("getAdminApiKeys", "isTenantAdmin()");
    }

    @Test
    void testCreateRequiresAuthenticated() {
        assertExpression("create", "isAuthenticated()");
    }

    @Test
    void testGetApiKeysRequiresAuthenticated() {
        assertExpression("getApiKeys", "isAuthenticated()");
    }

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : ApiKeyFacadeImpl.class.getDeclaredMethods()) {
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

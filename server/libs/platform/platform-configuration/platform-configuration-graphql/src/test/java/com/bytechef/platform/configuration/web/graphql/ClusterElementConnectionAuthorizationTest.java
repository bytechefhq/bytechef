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

package com.bytechef.platform.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that prevent using an arbitrary cross-workspace connection to fetch
 * cluster-element options/properties/fields (T18 follow-up). The check is null-safe: when no {@code connectionId} is
 * supplied there is nothing to authorize, otherwise the caller must be able to use the connection.
 *
 * @author Ivica Cardic
 */
class ClusterElementConnectionAuthorizationTest {

    private static final String EXPRESSION =
        "#connectionId == null or hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_USE')";

    @Test
    void testClusterElementOptionsGuardsConnection() {
        assertExpression(ClusterElementOptionGraphQlController.class, "clusterElementOptions");
    }

    @Test
    void testClusterElementDynamicPropertiesGuardsConnection() {
        assertExpression(ClusterElementDynamicPropertiesGraphQlController.class, "clusterElementDynamicProperties");
    }

    @Test
    void testClusterElementFieldsGuardsConnection() {
        assertExpression(ClusterElementFieldGraphQlController.class, "clusterElementFields");
    }

    private static void assertExpression(Class<?> controllerClass, String methodName) {
        Method method = null;

        for (Method candidate : controllerClass.getDeclaredMethods()) {
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
        assertThat(preAuthorize.value()).isEqualTo(EXPRESSION);
    }
}

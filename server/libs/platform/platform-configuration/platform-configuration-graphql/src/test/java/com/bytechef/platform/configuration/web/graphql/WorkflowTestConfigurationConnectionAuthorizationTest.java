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
 * Pins the {@code @PreAuthorize} that prevents binding an arbitrary cross-workspace connection to a workflow test
 * configuration (T22a). The connection-access dimension is gated here; the workflow-access dimension is part of T22.
 *
 * @author Ivica Cardic
 */
class WorkflowTestConfigurationConnectionAuthorizationTest {

    private static final String EXPRESSION =
        "hasPermission(#connectionId, 'Connection', 'CONNECTION_USE')";

    @Test
    void testSaveWorkflowTestConfigurationConnectionGuardsConnection() {
        assertExpression("saveWorkflowTestConfigurationConnection");
    }

    @Test
    void testSaveClusterElementTestConfigurationConnectionGuardsConnection() {
        assertExpression("saveClusterElementTestConfigurationConnection");
    }

    private static void assertExpression(String methodName) {
        Method method = null;

        for (Method candidate : WorkflowTestConfigurationGraphQlController.class.getDeclaredMethods()) {
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

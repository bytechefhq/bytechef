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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close the automation webhook-trigger-test IDOR (T30/B3). The
 * per-controller facade gates by workflow scope; the shared platform facade stays ungated for the embedded and runtime
 * callers.
 *
 * @author Ivica Cardic
 */
class WebhookTriggerTestApiFacadeAuthorizationTest {

    @Test
    void testEnableTriggerRequiresWorkflowEdit() {
        assertExpression("enableTrigger", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testDisableTriggerRequiresWorkflowEdit() {
        assertExpression("disableTrigger", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : WebhookTriggerTestApiFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {

                method = candidate;
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

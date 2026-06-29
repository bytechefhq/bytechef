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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the workflow-test-configuration write gates (T22). Each editor write resolves the owning project's workspace via
 * {@code hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')} (or {@code #workflowTestConfiguration
 * .workflowId} for the object overload). {@code removeUnusedWorkflowTestConfigurationConnections} is an internal
 * after-save event-listener cleanup (no user controller caller) and stays ungated -- a negative assertion locks that
 * in.
 *
 * @author Ivica Cardic
 */
class WorkflowTestConfigurationFacadeAuthorizationTest {

    @Test
    void testDeleteConnectionRequiresEdit() {
        assertExpression(
            "deleteWorkflowTestConfigurationConnection",
            "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testSaveConfigurationRequiresEdit() {
        assertExpression(
            "saveWorkflowTestConfiguration",
            "hasPermission(#workflowTestConfiguration.workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testSaveClusterElementConnectionRequiresEdit() {
        assertExpression(
            "saveClusterElementTestConfigurationConnection",
            "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testSaveConnectionRequiresEdit() {
        assertExpression(
            "saveWorkflowTestConfigurationConnection",
            "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testSaveInputsRequiresEdit() {
        assertExpression(
            "saveWorkflowTestConfigurationInputs",
            "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testRemoveUnusedConnectionsIsNotGated() {
        Method match = null;

        for (Method candidate : WorkflowTestConfigurationFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals("removeUnusedWorkflowTestConfigurationConnections")) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("removeUnusedWorkflowTestConfigurationConnections method")
            .isNotNull();
        assertThat(match.isAnnotationPresent(PreAuthorize.class))
            .as("internal after-save listener cleanup must NOT carry @PreAuthorize")
            .isFalse();
    }

    private static void assertExpression(String methodName, String expression) {
        Method match = null;

        for (Method candidate : WorkflowTestConfigurationFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method %s", methodName)
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo(expression);
    }
}

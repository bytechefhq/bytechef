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
 * Pins the {@code @PreAuthorize} expressions that close workflow-node-parameter IDOR (T22). Every operation keys on its
 * {@code workflowId} and resolves the owning project's workspace via {@code hasPermission(#workflowId, 'Workflow',
 * ...)}; reads require {@code WORKFLOW_VIEW}, mutations {@code WORKFLOW_EDIT}. These facade methods are invoked only by
 * the workflow-editor REST/GraphQL controllers (no worker/execution callers), so a per-workflow gate is safe.
 *
 * @author Ivica Cardic
 */
class WorkflowNodeParameterFacadeAuthorizationTest {

    @Test
    void testDeleteClusterElementParameterRequiresEdit() {
        assertExpression("deleteClusterElementParameter", "WORKFLOW_EDIT");
    }

    @Test
    void testDeleteWorkflowNodeParameterRequiresEdit() {
        assertExpression("deleteWorkflowNodeParameter", "WORKFLOW_EDIT");
    }

    @Test
    void testGetClusterElementDisplayConditionsRequiresView() {
        assertExpression("getClusterElementDisplayConditions", "WORKFLOW_VIEW");
    }

    @Test
    void testGetClusterElementMissingRequiredPropertiesRequiresView() {
        assertExpression("getClusterElementMissingRequiredProperties", "WORKFLOW_VIEW");
    }

    @Test
    void testGetWorkflowNodeDisplayConditionsRequiresView() {
        assertExpression("getWorkflowNodeDisplayConditions", "WORKFLOW_VIEW");
    }

    @Test
    void testGetWorkflowNodeMissingRequiredPropertiesRequiresView() {
        assertExpression("getWorkflowNodeMissingRequiredProperties", "WORKFLOW_VIEW");
    }

    @Test
    void testUpdateClusterElementParameterRequiresEdit() {
        assertExpression("updateClusterElementParameter", "WORKFLOW_EDIT");
    }

    @Test
    void testUpdateWorkflowNodeParameterRequiresEdit() {
        assertExpression("updateWorkflowNodeParameter", "WORKFLOW_EDIT");
    }

    private static void assertExpression(String methodName, String scope) {
        Method match = null;

        for (Method candidate : WorkflowNodeParameterFacadeImpl.class.getDeclaredMethods()) {
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
            .value()).isEqualTo("hasPermission(#workflowId, 'Workflow', '" + scope + "')");
    }
}

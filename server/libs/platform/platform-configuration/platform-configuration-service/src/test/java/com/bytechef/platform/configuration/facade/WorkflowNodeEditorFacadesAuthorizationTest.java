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
 * Pins the {@code @PreAuthorize} expressions that close workflow-node editor IDOR (T22) across the output, dynamic
 * properties, description, and script facades. Every operation keys on its {@code workflowId} and resolves the owning
 * project's workspace via {@code @permissionService.hasWorkflowScope}; reads require {@code WORKFLOW_VIEW}, script
 * test-executions require {@code WORKFLOW_EDIT}. These facades are invoked only by the workflow-editor controllers (no
 * worker/execution method callers), so per-workflow gates are safe.
 *
 * @author Ivica Cardic
 */
class WorkflowNodeEditorFacadesAuthorizationTest {

    @Test
    void testOutputFacadeReadsRequireView() {
        assertView(WorkflowNodeOutputFacadeImpl.class, "getClusterElementOutput");
        assertView(WorkflowNodeOutputFacadeImpl.class, "getWorkflowNodeOutput");
        assertView(WorkflowNodeOutputFacadeImpl.class, "getPreviousWorkflowNodeOutputs");
        assertView(WorkflowNodeOutputFacadeImpl.class, "getPreviousWorkflowNodeSampleOutputs");
        assertView(WorkflowNodeOutputFacadeImpl.class, "checkWorkflowCache");
    }

    @Test
    void testDynamicPropertiesReadsRequireView() {
        assertView(WorkflowNodeDynamicPropertiesFacadeImpl.class, "getClusterElementDynamicProperties");
        assertView(WorkflowNodeDynamicPropertiesFacadeImpl.class, "getWorkflowNodeDynamicProperties");
    }

    @Test
    void testDescriptionReadsRequireView() {
        assertView(WorkflowNodeDescriptionFacadeImpl.class, "getClusterElementWorkflowNodeDescription");
        assertView(WorkflowNodeDescriptionFacadeImpl.class, "getWorkflowNodeDescription");
    }

    @Test
    void testScriptInputReadsRequireViewAndTestsRequireEdit() {
        assertView(WorkflowNodeScriptFacadeImpl.class, "getClusterElementScriptInput");
        assertView(WorkflowNodeScriptFacadeImpl.class, "getWorkflowNodeScriptInput");
        assertEdit(WorkflowNodeScriptFacadeImpl.class, "testClusterElementScript");
        assertEdit(WorkflowNodeScriptFacadeImpl.class, "testWorkflowNodeScript");
    }

    private static void assertView(Class<?> clazz, String methodName) {
        assertScope(clazz, methodName, "WORKFLOW_VIEW");
    }

    private static void assertEdit(Class<?> clazz, String methodName) {
        assertScope(clazz, methodName, "WORKFLOW_EDIT");
    }

    private static void assertScope(Class<?> clazz, String methodName, String scope) {
        Method match = null;

        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method %s on %s", methodName, clazz.getSimpleName())
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("@permissionService.hasWorkflowScope(#workflowId, '" + scope + "')");
    }
}

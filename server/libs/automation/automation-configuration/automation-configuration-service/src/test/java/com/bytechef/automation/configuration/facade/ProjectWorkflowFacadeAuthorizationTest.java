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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close workflow IDOR (T22). Workflow-UUID operations resolve the
 * owning project's workspace via {@code hasPermission(#workflowId, 'Workflow', ...)}; project-id operations use the
 * {@code ProjectScope} token directly.
 *
 * @author Ivica Cardic
 */
class ProjectWorkflowFacadeAuthorizationTest {

    @Test
    void testAddWorkflowRequiresWorkflowCreate() {
        assertExpression("addWorkflow", "hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE')");
    }

    @Test
    void testDeleteWorkflowRequiresWorkflowDelete() {
        assertExpression("deleteWorkflow", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_DELETE')");
    }

    @Test
    void testDeleteSharedWorkflowRequiresWorkflowDelete() {
        assertExpression("deleteSharedWorkflow", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_DELETE')");
    }

    @Test
    void testExportSharedWorkflowRequiresWorkflowView() {
        assertExpression("exportSharedWorkflow", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')");
    }

    @Test
    void testGetProjectWorkflowByWorkflowIdRequiresWorkflowView() {
        assertExpression("getProjectWorkflow", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')",
            String.class);
    }

    @Test
    void testGetProjectWorkflowsByProjectIdRequiresWorkflowView() {
        assertExpression("getProjectWorkflows", "hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')",
            long.class);
    }

    @Test
    void testGetProjectVersionWorkflowsRequiresWorkflowView() {
        assertExpression(
            "getProjectVersionWorkflows", "hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')",
            long.class, int.class, boolean.class);
    }

    @Test
    void testImportWorkflowTemplateRequiresWorkflowCreate() {
        assertExpression("importWorkflowTemplate", "hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE')");
    }

    @Test
    void testUpdateWorkflowRequiresWorkflowEdit() {
        assertExpression("updateWorkflow", "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_EDIT')");
    }

    @Test
    void testDuplicateWorkflowRequiresCreateAndSourceView() {
        assertExpression(
            "duplicateWorkflow",
            "hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE') and "
                + "hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')");
    }

    private static void assertExpression(String methodName, String expression, Class<?>... parameterTypes) {
        Method method = findMethod(methodName, parameterTypes);

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        if (parameterTypes.length > 0) {
            for (Method candidate : ProjectWorkflowFacadeImpl.class.getDeclaredMethods()) {
                if (candidate.getName()
                    .equals(methodName) && parameterTypesMatch(candidate, parameterTypes)) {

                    return candidate;
                }
            }

            return null;
        }

        List<Method> matches = new java.util.ArrayList<>();

        for (Method candidate : ProjectWorkflowFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                matches.add(candidate);
            }
        }

        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private static boolean parameterTypesMatch(Method method, Class<?>... parameterTypes) {
        Class<?>[] actual = method.getParameterTypes();

        if (actual.length != parameterTypes.length) {
            return false;
        }

        for (int i = 0; i < actual.length; i++) {
            if (!actual[i].equals(parameterTypes[i])) {
                return false;
            }
        }

        return true;
    }
}

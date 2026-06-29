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

package com.bytechef.automation.ai.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacadeImpl;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowServiceImpl;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close MCP-project-workflow IDOR (T20) at the service/facade tier.
 * Reads require VIEWER, writes EDITOR, resolved via {@code McpProjectWorkflow:ResourceRole} (workflow &rarr; project
 * &rarr; server &rarr; workspace).
 *
 * @author Ivica Cardic
 */
class McpProjectWorkflowAuthorizationTest {

    @Test
    void testFetchRequiresViewer() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "fetchMcpProjectWorkflow",
            "hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_VIEW')");
    }

    @Test
    void testDeleteServiceRequiresEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "delete",
            "hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    @Test
    void testUpdateRequiresEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "update",
            "hasPermission(#id, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    @Test
    void testUpdateParametersRequiresEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "updateParameters",
            "hasPermission(#id, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    @Test
    void testFacadeDeleteRequiresEditor() {
        assertExpression(
            McpProjectWorkflowFacadeImpl.class, "deleteMcpProjectWorkflow",
            "hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    private static void assertExpression(Class<?> clazz, String methodName, String expression) {
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
            .value()).isEqualTo(expression);
    }
}

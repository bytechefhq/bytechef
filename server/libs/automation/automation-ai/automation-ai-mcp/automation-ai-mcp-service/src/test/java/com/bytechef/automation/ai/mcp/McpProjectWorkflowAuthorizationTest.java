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

import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacadeImpl;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowServiceImpl;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close MCP-project-workflow IDOR (T20) at the service/facade tier.
 * Reads require VIEWER, writes EDITOR, resolved via {@code McpProjectWorkflow:ResourceRole} (workflow &rarr; project
 * &rarr; server &rarr; workspace).
 * <p>
 * Creation has no row to authorize against yet, so it authorizes against the MCP project the row will belong to.
 * Re-pointing an existing row carries both checks: the row being edited and the MCP project it is being moved to -
 * without the second one an editor of any single row could graft it onto somebody else's project.
 * <p>
 * Every overload is pinned by its exact parameter types. Matching on the method name alone would let a guard sit on the
 * wrong overload of {@code create} or {@code update} and still pass, which is the same unguarded-sibling gap these
 * expressions exist to close.
 * <p>
 * The two list reads are deliberately unannotated - see {@link McpProjectWorkflowServiceImpl}'s class javadoc for why a
 * guard there would reject the MCP serve path, the delete-cascade listeners and the agent tool callback. What IS pinned
 * below is the absence of an unparameterised {@code getMcpProjectWorkflows()}: a no-argument read returning every row
 * cannot be guarded by id, so re-adding one would reopen the exposure that removing it closed.
 *
 * @author Ivica Cardic
 */
class McpProjectWorkflowAuthorizationTest {

    @Test
    void testCreateRequiresProjectEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "create", new Class<?>[] {
                Long.class, Long.class
            }, "hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')");
    }

    @Test
    void testCreateFromEntityRequiresProjectEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "create", new Class<?>[] {
                McpProjectWorkflow.class
            }, "hasPermission(#mcpProjectWorkflow.mcpProjectId, 'McpProject', 'MCP_EDIT')");
    }

    @Test
    void testFetchRequiresViewer() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "fetchMcpProjectWorkflow", new Class<?>[] {
                long.class
            }, "hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_VIEW')");
    }

    @Test
    void testDeleteServiceRequiresEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "delete", new Class<?>[] {
                long.class
            }, "hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    @Test
    void testUpdateRequiresEditorAndTargetProjectEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "update", new Class<?>[] {
                long.class, Long.class, Long.class
            },
            "hasPermission(#id, 'McpProjectWorkflow', 'MCP_EDIT') and " +
                "(#mcpProjectId == null or hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT'))");
    }

    @Test
    void testUpdateFromEntityRequiresEditorAndTargetProjectEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "update", new Class<?>[] {
                McpProjectWorkflow.class
            },
            "hasPermission(#mcpProjectWorkflow.id, 'McpProjectWorkflow', 'MCP_EDIT') and " +
                "hasPermission(#mcpProjectWorkflow.mcpProjectId, 'McpProject', 'MCP_EDIT')");
    }

    @Test
    void testUpdateParametersRequiresEditor() {
        assertExpression(
            McpProjectWorkflowServiceImpl.class, "updateParameters", new Class<?>[] {
                long.class, Map.class
            }, "hasPermission(#id, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    @Test
    void testFacadeDeleteRequiresEditor() {
        assertExpression(
            McpProjectWorkflowFacadeImpl.class, "deleteMcpProjectWorkflow", new Class<?>[] {
                long.class
            }, "hasPermission(#mcpProjectWorkflowId, 'McpProjectWorkflow', 'MCP_EDIT')");
    }

    @Test
    void testNoUnparameterisedReadExists() {
        assertThat(McpProjectWorkflowService.class.getDeclaredMethods())
            .as("an unparameterised read cannot be guarded by id and would return every row in the table")
            .noneMatch(method -> method.getName()
                .equals("getMcpProjectWorkflows") && method.getParameterCount() == 0);
        assertThat(McpProjectWorkflowServiceImpl.class.getDeclaredMethods())
            .as("an unparameterised read cannot be guarded by id and would return every row in the table")
            .noneMatch(method -> method.getName()
                .equals("getMcpProjectWorkflows") && method.getParameterCount() == 0);
    }

    private static void assertExpression(
        Class<?> clazz, String methodName, Class<?>[] parameterTypes, String expression) {

        Method match = Arrays.stream(clazz.getDeclaredMethods())
            .filter(candidate -> candidate.getName()
                .equals(methodName))
            .filter(candidate -> Arrays.equals(candidate.getParameterTypes(), parameterTypes))
            .findFirst()
            .orElse(null);

        assertThat(match)
            .as("method %s%s on %s", methodName, Arrays.toString(parameterTypes), clazz.getSimpleName())
            .isNotNull();

        PreAuthorize preAuthorize = match.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s%s", methodName, Arrays.toString(parameterTypes))
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}

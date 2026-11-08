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

package com.bytechef.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ToolErrorReportingToolSpecificationsTest {

    @Test
    void testBuildWorkflowWithoutWorkspaceIdIsReportedAsAnMcpError() {
        ToolCallback buildWorkflowToolCallback = toolCallback("buildWorkflow", "{\"result\":\"built\"}");

        McpSchema.CallToolResult callToolResult = callTool(
            new WorkspaceScopedSubAgentToolCallback(buildWorkflowToolCallback, mock(WorkspaceFacade.class)),
            Map.of("request", "Post new orders to a Slack channel"));

        assertThat(callToolResult.isError()).isTrue();
        assertThat(textOf(callToolResult)).contains("workspaceId is required");

        verify(buildWorkflowToolCallback, never()).call(
            ArgumentMatchers.anyString(), ArgumentMatchers.any(ToolContext.class));
    }

    @Test
    void testToolErrorEnvelopeIsReportedAsAnMcpError() {
        McpSchema.CallToolResult callToolResult = callTool(
            toolCallback("toggleProjectDeployment", "{\"error\":\"Project deployment 7 not found\"}"), Map.of());

        assertThat(callToolResult.isError()).isTrue();
        assertThat(textOf(callToolResult)).isEqualTo("{\"error\":\"Project deployment 7 not found\"}");
    }

    @Test
    void testSuccessfulResultIsNotAnMcpError() {
        McpSchema.CallToolResult callToolResult = callTool(
            toolCallback("listProjectDeployments", "[{\"id\":11,\"projectId\":7}]"), Map.of());

        assertThat(callToolResult.isError()).isFalse();
        assertThat(textOf(callToolResult)).isEqualTo("[{\"id\":11,\"projectId\":7}]");
    }

    @Test
    void testManagementServerToolSpecificationsReportToolErrors() {
        ToolCallback failingToolCallback = toolCallback("failingTool", "{\"error\":\"workspaceId is required\"}");

        ManagementMcpServerConfiguration managementMcpServerConfiguration = new ManagementMcpServerConfiguration(
            mock(ProjectTools.class), mock(ProjectWorkflowLifecycleTools.class), mock(WorkspaceTools.class),
            List.of(() -> List.of(failingToolCallback)));

        McpServerFeatures.AsyncToolSpecification toolSpecification = managementMcpServerConfiguration
            .toolSpecifications()
            .stream()
            .filter(specification -> "failingTool".equals(specification.tool()
                .name()))
            .findFirst()
            .orElseThrow();

        McpSchema.CallToolResult callToolResult = toolSpecification.callHandler()
            .apply(
                mock(McpAsyncServerExchange.class),
                McpSchema.CallToolRequest.builder()
                    .name("failingTool")
                    .arguments(Map.of())
                    .build())
            .block();

        assertThat(callToolResult.isError()).isTrue();
    }

    @Test
    void testPayloadCarryingAnErrorFieldAmongOtherDataIsNotAnMcpError() {
        McpSchema.CallToolResult callToolResult = callTool(
            toolCallback("getWorkflowExecution", "{\"id\":3,\"status\":\"FAILED\",\"error\":\"Task failed\"}"),
            Map.of());

        assertThat(callToolResult.isError()).isFalse();
        assertThat(textOf(callToolResult)).isEqualTo("{\"id\":3,\"status\":\"FAILED\",\"error\":\"Task failed\"}");
    }

    private static McpSchema.CallToolResult callTool(ToolCallback toolCallback, Map<String, Object> arguments) {
        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications =
            ToolErrorReportingToolSpecifications.toAsyncToolSpecifications(List.of(toolCallback));

        McpServerFeatures.AsyncToolSpecification toolSpecification = toolSpecifications.getFirst();

        McpSchema.Tool tool = toolSpecification.tool();

        McpSchema.CallToolRequest callToolRequest = McpSchema.CallToolRequest.builder()
            .name(tool.name())
            .arguments(arguments)
            .build();

        return toolSpecification.callHandler()
            .apply(mock(McpAsyncServerExchange.class), callToolRequest)
            .block();
    }

    private static String textOf(McpSchema.CallToolResult callToolResult) {
        McpSchema.TextContent textContent = (McpSchema.TextContent) callToolResult.content()
            .getFirst();

        return textContent.text();
    }

    private static ToolCallback toolCallback(String name, String result) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build());
        when(toolCallback.call(ArgumentMatchers.anyString())).thenReturn(result);
        when(toolCallback.call(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(result);

        return toolCallback;
    }
}

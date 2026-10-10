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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.dto.McpProjectWorkflowSummaryDTO;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ListMcpProjectWorkflowsToolCallbackTest {

    private McpProjectWorkflowFacade mcpProjectWorkflowFacade;
    private ListMcpProjectWorkflowsToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        mcpProjectWorkflowFacade = mock(McpProjectWorkflowFacade.class);
        toolCallback = new ListMcpProjectWorkflowsToolCallback(mcpProjectWorkflowFacade);
    }

    @Test
    void testToolDefinitionName() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("listMcpProjectWorkflows");
    }

    @Test
    void testMissingMcpServerIdReturnsError() {
        String result = toolCallback.call("{}");

        assertThat(result)
            .contains("error")
            .contains("mcpServerId is required");
    }

    @Test
    void testReturnsTheFacadeSummaries() {
        when(mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(5L)).thenReturn(
            List.of(
                new McpProjectWorkflowSummaryDTO(
                    300L, 400L, "workflow-1", "Weather Lookup", false, "get_weather", "Fetch weather",
                    Map.of("toolName", "get_weather"), null)));

        String result = toolCallback.call("{\"mcpServerId\": 5}");

        assertThat(result)
            .contains("\"mcpProjectWorkflowId\":400")
            .contains("get_weather")
            .contains("Weather Lookup")
            .contains("\"toolCallable\":false");
    }

    @Test
    void testSurfacesAFacadeFailureAsToolError() {
        when(mcpProjectWorkflowFacade.getMcpServerMcpProjectWorkflows(5L))
            .thenThrow(new IllegalArgumentException("McpServer not found"));

        assertThat(toolCallback.call("{\"mcpServerId\": 5}"))
            .contains("error")
            .contains("McpServer not found");
    }
}

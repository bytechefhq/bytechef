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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class UpdateMcpProjectWorkflowParametersToolCallbackTest {

    private McpProjectWorkflowFacade mcpProjectWorkflowFacade;
    private UpdateMcpProjectWorkflowParametersToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        mcpProjectWorkflowFacade = mock(McpProjectWorkflowFacade.class);
        toolCallback = new UpdateMcpProjectWorkflowParametersToolCallback(mcpProjectWorkflowFacade);
    }

    @Test
    void testToolDefinitionName() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("updateMcpProjectWorkflowParameters");
    }

    @Test
    void testMissingIdReturnsError() {
        String result = toolCallback.call("{\"toolName\": \"get_weather\"}");

        assertThat(result)
            .contains("error")
            .contains("mcpProjectWorkflowId is required");

        verify(mcpProjectWorkflowFacade, never()).updateMcpProjectWorkflowParameters(anyLong(), any(), any(), any());
    }

    @Test
    void testBlankToolNameReturnsError() {
        String result = toolCallback.call("{\"mcpProjectWorkflowId\": 1, \"toolName\": \"  \"}");

        assertThat(result)
            .contains("error")
            .contains("toolName must not be blank");

        verify(mcpProjectWorkflowFacade, never()).updateMcpProjectWorkflowParameters(anyLong(), any(), any(), any());
    }

    @Test
    void testUnknownIdReturnsError() {
        when(mcpProjectWorkflowFacade.updateMcpProjectWorkflowParameters(99L, "get_weather", null, null))
            .thenThrow(new IllegalArgumentException("McpProjectWorkflow not found: 99"));

        String result = toolCallback.call("{\"mcpProjectWorkflowId\": 99, \"toolName\": \"get_weather\"}");

        assertThat(result)
            .contains("error")
            .contains("McpProjectWorkflow not found");
    }

    @Test
    void testPassesTheSuppliedFieldsToTheFacade() {
        McpProjectWorkflow updatedMcpProjectWorkflow = new McpProjectWorkflow();

        updatedMcpProjectWorkflow.setParameters(Map.of("toolName", "get_weather", "city", "Berlin"));

        when(mcpProjectWorkflowFacade.updateMcpProjectWorkflowParameters(
            7L, "get_weather", "Fetch weather", Map.of("city", "Berlin")))
                .thenReturn(updatedMcpProjectWorkflow);

        String result = toolCallback.call(
            "{\"mcpProjectWorkflowId\": 7, \"toolName\": \"get_weather\", \"toolDescription\": \"Fetch weather\", "
                + "\"parameters\": {\"city\": \"Berlin\"}}");

        assertThat(result)
            .contains("get_weather")
            .contains("Berlin");
    }
}

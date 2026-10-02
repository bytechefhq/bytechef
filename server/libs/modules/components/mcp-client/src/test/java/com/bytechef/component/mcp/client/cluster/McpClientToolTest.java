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

package com.bytechef.component.mcp.client.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.mcp.client.util.McpClientUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
class McpClientToolTest {

    @Test
    void testToolCallKeepsTheConversationIdOutOfTheMcpMeta() throws Exception {
        McpSyncClient mcpSyncClient = mock(McpSyncClient.class);

        McpSchema.Tool tool = McpSchema.Tool.builder("search", Map.of("type", "object"))
            .build();

        when(mcpSyncClient.getClientCapabilities()).thenReturn(
            McpSchema.ClientCapabilities.builder()
                .build());
        when(mcpSyncClient.getClientInfo()).thenReturn(
            McpSchema.Implementation.builder("bytechef", "1.0")
                .build());
        when(mcpSyncClient.listTools()).thenReturn(
            McpSchema.ListToolsResult.builder(List.of(tool))
                .build());
        when(mcpSyncClient.callTool(any(McpSchema.CallToolRequest.class))).thenReturn(
            McpSchema.CallToolResult.builder()
                .textContent(List.of("found"))
                .build());

        Parameters inputParameters = MockParametersFactory.create(Map.of());
        Parameters connectionParameters = MockParametersFactory.create(Map.of());
        Context context = mock(Context.class);

        ToolCallbackProvider toolCallbackProvider;

        try (MockedStatic<McpClientUtils> mcpClientUtilsMockedStatic = mockStatic(
            McpClientUtils.class, CALLS_REAL_METHODS)) {

            mcpClientUtilsMockedStatic.when(() -> McpClientUtils.createMcpSyncClient(any(), any(), any()))
                .thenReturn(mcpSyncClient);

            ToolCallbackProviderFunction toolCallbackProviderFunction = new McpClientTool(null)
                .getClusterElementDefinition()
                .getElement();

            toolCallbackProvider = toolCallbackProviderFunction.apply(inputParameters, connectionParameters, context);
        }

        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();

        assertThat(toolCallbacks).hasSize(1);

        toolCallbacks[0].call(
            "{}",
            new ToolContext(Map.of(ChatMemory.CONVERSATION_ID, "conversation-1", "tenantHint", "tenant-1")));

        ArgumentCaptor<McpSchema.CallToolRequest> callToolRequestArgumentCaptor = ArgumentCaptor.forClass(
            McpSchema.CallToolRequest.class);

        verify(mcpSyncClient).callTool(callToolRequestArgumentCaptor.capture());

        McpSchema.CallToolRequest callToolRequest = callToolRequestArgumentCaptor.getValue();

        assertThat(callToolRequest.meta())
            .doesNotContainKey(ChatMemory.CONVERSATION_ID)
            .containsEntry("tenantHint", "tenant-1");
    }
}

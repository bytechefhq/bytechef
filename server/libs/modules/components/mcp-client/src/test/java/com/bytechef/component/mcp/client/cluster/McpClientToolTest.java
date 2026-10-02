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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.ai.tool.AiAgentToolContext;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
class McpClientToolTest {

    @Test
    void testToolCallDoesNotSendTheAgentToolContextToTheMcpServer() {
        McpSyncClient mcpSyncClient = mock(McpSyncClient.class);

        when(mcpSyncClient.getClientCapabilities())
            .thenReturn(ClientCapabilities.builder()
                .build());
        when(mcpSyncClient.getClientInfo())
            .thenReturn(new Implementation("bytechef", "1.0"));
        when(mcpSyncClient.listTools())
            .thenReturn(new ListToolsResult(List.of(Tool.builder("search", Map.of("type", "object"))
                .build()), null, null));
        when(mcpSyncClient.callTool(any(CallToolRequest.class)))
            .thenReturn(new CallToolResult(List.of(), false, null, null));

        ToolCallback[] toolCallbacks = McpClientTool.createToolCallbackProvider(mcpSyncClient, (info, tool) -> true)
            .getToolCallbacks();

        ToolContext toolContext = new ToolContext(
            new AiAgentToolContext(mock(ActionContext.class), new AiAgentToolContext.SseTransport()).toMap());

        toolCallbacks[0].call("{}", toolContext);

        ArgumentCaptor<CallToolRequest> callToolRequestArgumentCaptor = ArgumentCaptor.forClass(CallToolRequest.class);

        verify(mcpSyncClient).callTool(callToolRequestArgumentCaptor.capture());

        CallToolRequest callToolRequest = callToolRequestArgumentCaptor.getValue();

        assertThat(callToolRequest.meta()).isNullOrEmpty();
    }
}

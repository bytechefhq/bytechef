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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConfigureMcpServerToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsResultWhenSubagentSucceeds() {
        String synthesised = "Mapped 2 workflows: get_weather, create_invoice";

        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(synthesised);

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"mcpServerId\":42}");

        assertThat(result).isEqualTo(synthesised);
    }

    @Test
    void testCallIncludesInstructionInRequestWhenSupplied() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        callback.call("{\"mcpServerId\":42,\"instruction\":\"name them all get_*\"}");

        verify(chatClient).prompt("Complete the tool mapping for every workflow already attached to MCP server 42."
            + " name them all get_*");
    }

    @Test
    void testCallReturnsErrorWhenMcpServerIdIsMissing() {
        ConfigureMcpServerToolCallback callback =
            new ConfigureMcpServerToolCallback(chatModel -> mock(ChatClient.class), null);

        String result = callback.call("{}");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("mcpServerId is required");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        ConfigureMcpServerToolCallback callback =
            new ConfigureMcpServerToolCallback(chatModel -> mock(ChatClient.class), null);

        String result = callback.call("not-json");

        assertThat(result).contains("error");
        assertThat(result).containsIgnoringCase("invalid tool input");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"mcpServerId\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).containsIgnoringCase("returned null");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("mcp service unavailable"));

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"mcpServerId\":42}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name")
                .contains("configureMcpServer failed")
                .as("payload must NOT leak the exception getMessage()")
                .doesNotContain("mcp service unavailable");
    }

    @Test
    void testCallForwardsParentToolContextToSubagent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        Map<String, Object> parentContextMap = Map.of("workspaceId", 11L, "userId", 42L);

        ToolContext parentToolContext = new ToolContext(parentContextMap);

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        callback.call("{\"mcpServerId\":42}", parentToolContext);

        verify(requestSpec).toolContext(parentContextMap);
    }

    @Test
    void testCallForwardsEmptyMapWhenParentToolContextIsNull() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        stubToolContext(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("ok");

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        callback.call("{\"mcpServerId\":42}", null);

        verify(requestSpec).toolContext(Map.of());
    }

    @Test
    void testToolDefinitionExposesConfigureMcpServerNameAndSchema() {
        ConfigureMcpServerToolCallback callback =
            new ConfigureMcpServerToolCallback(chatModel -> mock(ChatClient.class), null);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("configureMcpServer");
        assertThat(callback.getToolDefinition()
            .inputSchema()).contains("\"mcpServerId\"")
                .contains("\"instruction\"");
    }

    private static void stubToolContext(ChatClientRequestSpec requestSpec) {
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
    }
}

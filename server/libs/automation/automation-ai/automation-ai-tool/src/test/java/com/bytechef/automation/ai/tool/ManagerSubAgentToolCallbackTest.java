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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;

/**
 *
 * @author Ivica Cardic
 */
class ManagerSubAgentToolCallbackTest {

    @Test
    void testToolDefinitionCarriesAgentTypeKey() {
        ManagerSubAgentToolCallback toolCallback = new ManagerSubAgentToolCallback(
            ManagerAgentType.MCP_MANAGER, mock(ChatClient.class), "Manages MCP servers.");

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("mcp_manager");
        assertThat(toolCallback.getToolDefinition()
            .description()).isEqualTo("Manages MCP servers.");
    }

    @Test
    void testBlankRequestReturnsError() {
        ManagerSubAgentToolCallback toolCallback = new ManagerSubAgentToolCallback(
            ManagerAgentType.MCP_MANAGER, mock(ChatClient.class), "Manages MCP servers.");

        String result = toolCallback.call("{\"request\": \"  \"}");

        assertThat(result).contains("error");
        assertThat(result).contains("request is required");
    }

    @Test
    void testDelegatesToChatClientAndReturnsResponse() {
        ChatClient chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);

        when(chatClient.prompt("expose my weather workflow over MCP")
            .toolContext(Map.of())
            .call()
            .content()).thenReturn("Server 5 configured; tool get_weather mapped.");

        ManagerSubAgentToolCallback toolCallback = new ManagerSubAgentToolCallback(
            ManagerAgentType.MCP_MANAGER, chatClient, "Manages MCP servers.");

        String result = toolCallback.call("{\"request\": \"expose my weather workflow over MCP\"}");

        assertThat(result).isEqualTo("Server 5 configured; tool get_weather mapped.");
    }
}

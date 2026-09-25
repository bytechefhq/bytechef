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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.agent.SelectedChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConfigureMcpServerToolCallbackTest {

    @Test
    void testCallSendsTheMcpServerIdAndInstructionToTheSubagent() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);
        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt(requestCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("configured");

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"mcpServerId\": 7, \"instruction\": \"Prefer short names.\"}");

        assertThat(result).isEqualTo("configured");
        assertThat(requestCaptor.getValue())
            .contains("MCP server 7")
            .endsWith("Prefer short names.");
    }

    @Test
    void testCallReturnsErrorWhenMcpServerIdIsMissing() {
        ConfigureMcpServerToolCallback callback =
            new ConfigureMcpServerToolCallback(chatModel -> mock(ChatClient.class), null);

        String result = callback.call("{}");

        assertThat(result).contains("error")
            .contains("mcpServerId is required");
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(chatModel -> chatClient, null);

        String result = callback.call("{\"mcpServerId\": 7}");

        assertThat(result).contains("configureMcpServer subagent returned null");
    }

    @Test
    void testCallFailsTheTurnWhenTheSelectedModelIsUnavailable() {
        AtomicReference<Boolean> factoryInvoked = new AtomicReference<>(false);

        SubAgentChatModelResolver chatModelResolver = toolContext -> {
            throw new SelectedChatModelUnavailableException("anthropic", "claude-opus-4", 1);
        };

        ConfigureMcpServerToolCallback callback = new ConfigureMcpServerToolCallback(
            chatModel -> {
                factoryInvoked.set(true);

                return mock(ChatClient.class);
            },
            chatModelResolver);

        assertThatThrownBy(() -> callback.call("{\"mcpServerId\": 7}", new ToolContext(Map.of())))
            .isInstanceOf(ChatModelUnavailableException.class)
            .hasMessageContaining("claude-opus-4");

        assertThat(factoryInvoked.get()).isFalse();
    }
}

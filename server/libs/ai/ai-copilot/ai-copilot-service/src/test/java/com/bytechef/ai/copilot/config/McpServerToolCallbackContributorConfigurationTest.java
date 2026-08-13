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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class McpServerToolCallbackContributorConfigurationTest {

    private final ToolCallbackContributorConfiguration configuration =
        new ToolCallbackContributorConfiguration();

    @Test
    void contributesAgentCallbacksWhenChatClientsPresent() {
        ChatClient converterChatClient = mock(ChatClient.class);

        Supplier<ChatClient> converterChatClientSupplier = () -> converterChatClient;

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(converterChatClientSupplier), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), mock(WorkspaceService.class));

        // Asserting names (not just a count) catches a provider wired to the wrong callback type — every
        // contributed callback is a WorkspaceScopedSubAgentToolCallback, so a mis-wire (e.g. the asset-file
        // provider passed to DataTableAgentToolCallback) would still pass a bare hasSize check.
        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "workflow_editor_agent", "code_editor_agent", "cluster_element_agent", "skills_agent",
                "workflow_execution_agent", "converter_agent", "knowledge_base_agent", "data_table_agent",
                "ai_agent_agent", "asset_file_agent");
    }

    @Test
    void contributesNothingWhenAllAbsent() {
        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(),
            emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(),
            mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void contributedAgentToolsAcceptWorkspaceId() {
        ChatClient converterChatClient = mock(ChatClient.class);

        Supplier<ChatClient> converterChatClientSupplier = () -> converterChatClient;

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(converterChatClientSupplier), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            Consumer<T> consumer = invocation.getArgument(0);

            consumer.accept(value);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}

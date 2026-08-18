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
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class McpServerToolCallbackContributorConfigurationTest {

    private final ToolCallbackContributorConfiguration configuration =
        new ToolCallbackContributorConfiguration();

    @Test
    void contributesAgentCallbacksWhenChatClientsPresent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), intelligentToolCatalog,
            mock(WorkspaceService.class));

        // Asserting names (not just a count) catches two different classes of bug. For the four directly-wired
        // names (knowledge_base_agent, data_table_agent, ai_agent_agent, asset_file_agent) it catches a provider
        // wired to the wrong callback type in THIS class — every one of those is built by hand here, so a mis-wire
        // (e.g. the asset-file provider passed to DataTableAgentToolCallback) would still pass a bare hasSize
        // check. For the six catalog-sourced names it instead pins that copilotAgentToolCallbackContributor's
        // getByNames call correctly passes the fed-in fake definitions through to its output; the real
        // ChatClient-to-callback wiring for those six is CopilotIntelligentToolContributorTest's concern, since the
        // fakes here return a canned ToolCallback regardless of how it was built.
        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "buildWorkflow", "writeScript", "configureClusterElement", "authorSkill",
                "debugWorkflowExecution", "importWorkflow", "knowledge_base_agent", "data_table_agent",
                "ai_agent_agent", "asset_file_agent");
    }

    @Test
    void contributesNothingWhenAllAbsent() {
        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), emptyProvider(), catalogOf(),
            mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void contributedAgentToolsAcceptWorkspaceId() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), present(mock(ChatClient.class)), present(mock(ChatClient.class)),
            present(mock(ChatClient.class)), present(mock(ChatClient.class)), intelligentToolCatalog,
            mock(WorkspaceService.class));

        assertThat(contributor.getToolCallbacks())
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).contains("workspaceId"));
    }

    private static IntelligentToolDefinition intelligentDefinition(String name) {
        ChatClient chatClient = mock(ChatClient.class);
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition())
            .thenReturn(ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{}")
                .build());

        return new FakeIntelligentToolDefinition(
            name, Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient),
            toolCallback);
    }

    private static IntelligentToolCatalog catalogOf(IntelligentToolDefinition... definitions) {
        IntelligentToolContributor contributor = () -> List.of(definitions);

        return new IntelligentToolCatalog(fixedObjectProvider(contributor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedObjectProvider(
        IntelligentToolContributor contributor) {

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return objectProvider;
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

    private static final class FakeIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant;
        private final ToolCallback toolCallback;

        private FakeIntelligentToolDefinition(
            String name, Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant,
            ToolCallback toolCallback) {

            this.name = name;
            this.chatClientFactoriesByVariant = chatClientFactoriesByVariant;
            this.toolCallback = toolCallback;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String agentTypeKey() {
            return name;
        }

        @Override
        public Set<IntelligentToolScope> panelScopes() {
            return Set.of();
        }

        @Override
        @Nullable
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return chatClientFactoriesByVariant.get(variant);
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            return toolCallback;
        }
    }
}

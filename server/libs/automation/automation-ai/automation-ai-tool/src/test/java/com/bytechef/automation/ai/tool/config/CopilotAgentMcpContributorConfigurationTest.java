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

package com.bytechef.automation.ai.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalogImpl;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.tool.AutomationConfigurationAgentType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class CopilotAgentMcpContributorConfigurationTest {

    private final CopilotAgentMcpContributorConfiguration configuration =
        new CopilotAgentMcpContributorConfiguration();
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @Test
    void contributesAgentCallbacksWhenChatClientsPresent() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, workspaceFacade);

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder("buildWorkflow", "authorSkill", "debugWorkflowExecution", "importWorkflow");
    }

    @Test
    void testCopilotAgentContributorRendersSpecialistQuestionsAsPlainText() {
        IntelligentToolCatalog intelligentToolCatalog = mock(IntelligentToolCatalog.class);

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, workspaceFacade);

        contributor.getToolCallbacks();

        verify(intelligentToolCatalog).getByNames(
            eq(CopilotAgentMcpContributorConfiguration.INTELLIGENT_TOOL_NAMES), eq(IntelligentToolVariant.BUILD),
            any(), any(), eq(SubAgentQuestionRenderer.PLAIN_TEXT));
    }

    @Test
    void contributesNothingWhenAllAbsent() {
        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), catalogOf(), workspaceFacade);

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void contributedAgentToolsAcceptWorkspaceId() {
        IntelligentToolCatalog intelligentToolCatalog = catalogOf(
            intelligentDefinition("buildWorkflow"), intelligentDefinition("writeScript"),
            intelligentDefinition("configureClusterElement"), intelligentDefinition("authorSkill"),
            intelligentDefinition("debugWorkflowExecution"), intelligentDefinition("importWorkflow"));

        McpServerToolCallbackContributor contributor = configuration.copilotAgentToolCallbackContributor(
            emptyProvider(), intelligentToolCatalog, workspaceFacade);

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

        return new IntelligentToolCatalogImpl(fixedObjectProvider(contributor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedObjectProvider(
        IntelligentToolContributor contributor) {

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return objectProvider;
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
        public AgentType agentType() {
            return AutomationConfigurationAgentType.BUILD_WORKFLOW;
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

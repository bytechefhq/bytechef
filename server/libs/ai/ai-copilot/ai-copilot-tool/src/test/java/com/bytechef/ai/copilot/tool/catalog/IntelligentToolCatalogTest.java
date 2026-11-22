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

package com.bytechef.ai.copilot.tool.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class IntelligentToolCatalogTest {

    @Test
    void testGetByNamesSkipsDefinitionWithNoSupplierForAskVariant() {
        FakeIntelligentToolDefinition projectWorkflowAgent = projectScopedDefinition();
        FakeIntelligentToolDefinition mcpAgent = unscopedDefinition();
        FakeIntelligentToolDefinition converterAgent = buildOnlyDefinition();

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent, mcpAgent, converterAgent);

        List<ToolCallback> toolCallbacks = catalog.getByNames(
            Set.of(projectWorkflowAgent.name, mcpAgent.name, converterAgent.name), IntelligentToolVariant.ASK,
            identityChatClientDecorator(), identityToolCallbackDecorator());

        assertThat(toolCallbacks).containsExactly(projectWorkflowAgent.toolCallback, mcpAgent.toolCallback);
    }

    @Test
    void testGetForPanelReturnsOnlyDefinitionsScopedToTheGivenPanel() {
        FakeIntelligentToolDefinition projectWorkflowAgent = projectScopedDefinition();
        FakeIntelligentToolDefinition mcpAgent = unscopedDefinition();
        FakeIntelligentToolDefinition converterAgent = buildOnlyDefinition();

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent, mcpAgent, converterAgent);

        List<ToolCallback> toolCallbacks = catalog.getForPanel(
            IntelligentToolScope.PROJECT, IntelligentToolVariant.BUILD,
            identityChatClientDecorator(), identityToolCallbackDecorator());

        assertThat(toolCallbacks).containsExactly(projectWorkflowAgent.toolCallback);
    }

    @Test
    void testGetByNamesReturnsOnlyTheMatchingDefinition() {
        FakeIntelligentToolDefinition projectWorkflowAgent = projectScopedDefinition();
        FakeIntelligentToolDefinition mcpAgent = unscopedDefinition();
        FakeIntelligentToolDefinition converterAgent = buildOnlyDefinition();

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent, mcpAgent, converterAgent);

        List<ToolCallback> toolCallbacks = catalog.getByNames(
            Set.of(mcpAgent.name), IntelligentToolVariant.BUILD,
            identityChatClientDecorator(), identityToolCallbackDecorator());

        assertThat(toolCallbacks).containsExactly(mcpAgent.toolCallback);
    }

    @Test
    void testDecoratorsAreInvokedWithTheMatchingDefinition() {
        FakeIntelligentToolDefinition projectWorkflowAgent = projectScopedDefinition();

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent);

        List<IntelligentToolDefinition> chatClientDecoratorCalls = new ArrayList<>();
        List<IntelligentToolDefinition> toolCallbackDecoratorCalls = new ArrayList<>();

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            (chatClient, definition) -> {
                chatClientDecoratorCalls.add(definition);

                return chatClient;
            };

        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> toolCallbackDecorator =
            (toolCallback, definition) -> {
                toolCallbackDecoratorCalls.add(definition);

                return toolCallback;
            };

        catalog.getForPanel(
            IntelligentToolScope.PROJECT, IntelligentToolVariant.BUILD, chatClientDecorator, toolCallbackDecorator);

        assertThat(chatClientDecoratorCalls).containsExactly(projectWorkflowAgent);
        assertThat(toolCallbackDecoratorCalls).containsExactly(projectWorkflowAgent);
    }

    @Test
    void testChatClientDecoratorResultIsPassedToDefinitionCreate() {
        ChatClient rawChatClient = mock(ChatClient.class);
        ChatClient decoratedChatClient = mock(ChatClient.class);

        FakeIntelligentToolDefinition projectWorkflowAgent = projectScopedDefinition(rawChatClient);

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent);

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            (chatClient, definition) -> decoratedChatClient;

        catalog.getForPanel(
            IntelligentToolScope.PROJECT, IntelligentToolVariant.BUILD, chatClientDecorator,
            identityToolCallbackDecorator());

        assertThat(projectWorkflowAgent.capturedChatClient).isSameAs(decoratedChatClient);
    }

    @Test
    void testBuildToolCallbackDoesNotResolveTheRawChatClientSupplierEagerly() {
        AtomicInteger rawFactoryResolutionCount = new AtomicInteger();
        IntelligentToolChatClientFactory rawChatClientFactory = chatModel -> {
            rawFactoryResolutionCount.incrementAndGet();

            return mock(ChatClient.class);
        };

        ToolCallback toolCallback = mock(ToolCallback.class);
        LazyIntelligentToolDefinition converterAgent = new LazyIntelligentToolDefinition(
            "importWorkflow", rawChatClientFactory, toolCallback);

        IntelligentToolCatalog catalog = catalogOf(converterAgent);

        List<ToolCallback> toolCallbacks = catalog.getByNames(
            Set.of(converterAgent.name), IntelligentToolVariant.BUILD, identityChatClientDecorator(),
            identityToolCallbackDecorator());

        assertThat(toolCallbacks).containsExactly(toolCallback);
        assertThat(rawFactoryResolutionCount)
            .as("the catalog must hand definition.create() an unresolved factory — resolving it eagerly would"
                + " defeat importWorkflow's deliberately lazy ChatClient construction")
            .hasValue(0);
    }

    @Test
    void testTheChatModelReachesTheDefinitionFactory() {
        AtomicReference<ChatModel> received = new AtomicReference<>();

        IntelligentToolDefinition definition = definitionWithFactory(
            candidate -> {
                received.set(candidate);

                return mock(ChatClient.class);
            });

        List<ToolCallback> toolCallbacks = catalogOf(definition)
            .getByNames(
                Set.of("test_tool"), IntelligentToolVariant.BUILD, (chatClient, current) -> chatClient,
                (toolCallback, current) -> toolCallback);

        assertThat(toolCallbacks).hasSize(1);

        assertThat(received.get()).isNull();
    }

    private static FakeIntelligentToolDefinition projectScopedDefinition() {
        return projectScopedDefinition(mock(ChatClient.class));
    }

    private static FakeIntelligentToolDefinition projectScopedDefinition(ChatClient chatClient) {
        return new FakeIntelligentToolDefinition(
            "buildWorkflow", CopilotAgentType.BUILD_WORKFLOW, Set.of(IntelligentToolScope.PROJECT),
            Map.of(
                IntelligentToolVariant.ASK, (IntelligentToolChatClientFactory) chatModel -> chatClient,
                IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient));
    }

    private static FakeIntelligentToolDefinition unscopedDefinition() {
        ChatClient chatClient = mock(ChatClient.class);

        return new FakeIntelligentToolDefinition(
            "mcp_agent", CopilotAgentType.CONFIGURE_MCP_SERVER, Set.of(),
            Map.of(
                IntelligentToolVariant.ASK, (IntelligentToolChatClientFactory) chatModel -> chatClient,
                IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient));
    }

    private static FakeIntelligentToolDefinition buildOnlyDefinition() {
        ChatClient chatClient = mock(ChatClient.class);

        return new FakeIntelligentToolDefinition(
            "importWorkflow", CopilotAgentType.IMPORT_WORKFLOW, Set.of(),
            Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient));
    }

    private static IntelligentToolDefinition definitionWithFactory(IntelligentToolChatClientFactory chatClientFactory) {
        return new LazyIntelligentToolDefinition("test_tool", chatClientFactory, mock(ToolCallback.class));
    }

    private static BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> identityChatClientDecorator() {
        return (chatClient, definition) -> chatClient;
    }

    private static BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> identityToolCallbackDecorator() {
        return (toolCallback, definition) -> toolCallback;
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

    private static final class FakeIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final AgentType agentType;
        private final Set<IntelligentToolScope> panelScopes;
        private final Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant;
        private final ToolCallback toolCallback = mock(ToolCallback.class);

        @Nullable
        private ChatClient capturedChatClient;

        private FakeIntelligentToolDefinition(
            String name, AgentType agentType, Set<IntelligentToolScope> panelScopes,
            Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant) {

            this.name = name;
            this.agentType = agentType;
            this.panelScopes = panelScopes;
            this.chatClientFactoriesByVariant = chatClientFactoriesByVariant;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public AgentType agentType() {
            return agentType;
        }

        @Override
        @SuppressFBWarnings("EI_EXPOSE_REP")
        public Set<IntelligentToolScope> panelScopes() {
            return panelScopes;
        }

        @Override
        @Nullable
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return chatClientFactoriesByVariant.get(variant);
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            capturedChatClient = chatClientFactory.get(null);

            return toolCallback;
        }
    }

    private static final class LazyIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final IntelligentToolChatClientFactory rawChatClientFactory;
        private final ToolCallback toolCallback;

        private LazyIntelligentToolDefinition(
            String name, IntelligentToolChatClientFactory rawChatClientFactory, ToolCallback toolCallback) {

            this.name = name;
            this.rawChatClientFactory = rawChatClientFactory;
            this.toolCallback = toolCallback;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public AgentType agentType() {
            return CopilotAgentType.BUILD_WORKFLOW;
        }

        @Override
        public Set<IntelligentToolScope> panelScopes() {
            return Set.of();
        }

        @Override
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return rawChatClientFactory;
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            return toolCallback;
        }
    }
}

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class IntelligentToolCatalogTest {

    @Test
    void testGetByNamesSkipsDefinitionWithNoFactoryForVariant() {
        FakeIntelligentToolDefinition projectWorkflowAgent = buildDefinition("buildWorkflow", mock(ChatClient.class));
        FakeIntelligentToolDefinition mcpAgent = buildDefinition("mcp_agent", mock(ChatClient.class));
        FakeIntelligentToolDefinition unavailableAgent = new FakeIntelligentToolDefinition("importWorkflow", Map.of());

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent, mcpAgent, unavailableAgent);

        List<ToolCallback> toolCallbacks = catalog.getByNames(
            Set.of(projectWorkflowAgent.name, mcpAgent.name, unavailableAgent.name), IntelligentToolVariant.BUILD,
            identityChatClientDecorator(), identityToolCallbackDecorator());

        assertThat(toolCallbacks).containsExactly(projectWorkflowAgent.toolCallback, mcpAgent.toolCallback);
    }

    @Test
    void testGetByNamesReturnsOnlyTheMatchingDefinition() {
        FakeIntelligentToolDefinition projectWorkflowAgent = buildDefinition("buildWorkflow", mock(ChatClient.class));
        FakeIntelligentToolDefinition mcpAgent = buildDefinition("mcp_agent", mock(ChatClient.class));
        FakeIntelligentToolDefinition converterAgent = buildDefinition("importWorkflow", mock(ChatClient.class));

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent, mcpAgent, converterAgent);

        List<ToolCallback> toolCallbacks = catalog.getByNames(
            Set.of(mcpAgent.name), IntelligentToolVariant.BUILD,
            identityChatClientDecorator(), identityToolCallbackDecorator());

        assertThat(toolCallbacks).containsExactly(mcpAgent.toolCallback);
    }

    @Test
    void testDecoratorsAreInvokedWithTheMatchingDefinition() {
        FakeIntelligentToolDefinition projectWorkflowAgent = buildDefinition("buildWorkflow", mock(ChatClient.class));

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

        catalog.getByNames(
            Set.of(projectWorkflowAgent.name), IntelligentToolVariant.BUILD, chatClientDecorator,
            toolCallbackDecorator);

        assertThat(chatClientDecoratorCalls).containsExactly(projectWorkflowAgent);
        assertThat(toolCallbackDecoratorCalls).containsExactly(projectWorkflowAgent);
    }

    @Test
    void testChatClientDecoratorResultIsPassedToDefinitionCreate() {
        ChatClient rawChatClient = mock(ChatClient.class);
        ChatClient decoratedChatClient = mock(ChatClient.class);

        FakeIntelligentToolDefinition projectWorkflowAgent = buildDefinition("buildWorkflow", rawChatClient);

        IntelligentToolCatalog catalog = catalogOf(projectWorkflowAgent);

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            (chatClient, definition) -> decoratedChatClient;

        catalog.getByNames(
            Set.of(projectWorkflowAgent.name), IntelligentToolVariant.BUILD, chatClientDecorator,
            identityToolCallbackDecorator());

        assertThat(projectWorkflowAgent.capturedChatClient).isSameAs(decoratedChatClient);
    }

    @Test
    void testBuildToolCallbackDoesNotResolveTheRawChatClientFactoryEagerly() {
        AtomicInteger rawFactoryResolutionCount = new AtomicInteger();
        IntelligentToolChatClientFactory rawChatClientFactory = () -> {
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

    private static FakeIntelligentToolDefinition buildDefinition(String name, ChatClient chatClient) {
        return new FakeIntelligentToolDefinition(
            name, Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) () -> chatClient));
    }

    private static BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> identityChatClientDecorator() {
        return (chatClient, definition) -> chatClient;
    }

    private static BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> identityToolCallbackDecorator() {
        return (toolCallback, definition) -> toolCallback;
    }

    @SuppressWarnings("unchecked")
    private static IntelligentToolCatalog catalogOf(IntelligentToolDefinition... definitions) {
        IntelligentToolContributor contributor = () -> List.of(definitions);

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return new IntelligentToolCatalog(objectProvider);
    }

    private static final class FakeIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant;
        private final ToolCallback toolCallback = mock(ToolCallback.class);

        @Nullable
        private ChatClient capturedChatClient;

        private FakeIntelligentToolDefinition(
            String name, Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant) {

            this.name = name;
            this.chatClientFactoriesByVariant = chatClientFactoriesByVariant;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        @Nullable
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return chatClientFactoriesByVariant.get(variant);
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            capturedChatClient = chatClientFactory.get();

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
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return rawChatClientFactory;
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            return toolCallback;
        }
    }
}

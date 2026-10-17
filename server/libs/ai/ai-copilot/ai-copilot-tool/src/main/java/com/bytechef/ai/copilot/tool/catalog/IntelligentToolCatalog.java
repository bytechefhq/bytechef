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

import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelayToolCallback;
import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class IntelligentToolCatalog {

    private final List<IntelligentToolDefinition> definitions;
    private final @Nullable SubAgentAskRelay askRelay;

    public IntelligentToolCatalog(ObjectProvider<IntelligentToolContributor> contributors) {
        this(contributors, (SubAgentAskRelay) null);
    }

    @Autowired
    public IntelligentToolCatalog(
        ObjectProvider<IntelligentToolContributor> contributors, ObjectProvider<SubAgentAskRelay> askRelayProvider) {

        this(contributors, askRelayProvider.getIfAvailable());
    }

    private IntelligentToolCatalog(
        ObjectProvider<IntelligentToolContributor> contributors, @Nullable SubAgentAskRelay askRelay) {

        this.definitions = contributors.orderedStream()
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .toList();
        this.askRelay = askRelay;
    }

    public List<ToolCallback> getByNames(
        Set<String> names,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        return getByNames(names, variant, chatClientDecorator, callbackDecorator, SubAgentQuestionRenderer.JSON);
    }

    public List<ToolCallback> getByNames(
        Set<String> names,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator,
        SubAgentQuestionRenderer questionRenderer) {

        List<IntelligentToolDefinition> matchingDefinitions = definitions.stream()
            .filter(definition -> names.contains(definition.name()))
            .toList();

        return buildToolCallbacks(
            matchingDefinitions, variant, chatClientDecorator, callbackDecorator, questionRenderer);
    }

    public List<ToolCallback> getForPanel(
        IntelligentToolScope scope,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        List<IntelligentToolDefinition> matchingDefinitions = definitions.stream()
            .filter(definition -> definition.panelScopes()
                .contains(scope))
            .toList();

        return buildToolCallbacks(
            matchingDefinitions, variant, chatClientDecorator, callbackDecorator, SubAgentQuestionRenderer.JSON);
    }

    public List<String> getNames() {
        return definitions.stream()
            .map(IntelligentToolDefinition::name)
            .toList();
    }

    private List<ToolCallback> buildToolCallbacks(
        List<IntelligentToolDefinition> candidateDefinitions,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator,
        SubAgentQuestionRenderer questionRenderer) {

        return candidateDefinitions.stream()
            .map(
                definition -> buildToolCallback(
                    definition, variant, chatClientDecorator, callbackDecorator, questionRenderer))
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    private ToolCallback buildToolCallback(
        IntelligentToolDefinition definition,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator,
        SubAgentQuestionRenderer questionRenderer) {

        IntelligentToolChatClientFactory rawChatClientFactory = definition.chatClientFactory(variant);

        if (rawChatClientFactory == null) {
            return null;
        }

        boolean askCapable = definition.askCapable();

        IntelligentToolChatClientFactory decoratedChatClientFactory =
            chatModel -> chatClientDecorator.apply(
                withAskTool(rawChatClientFactory.get(chatModel), askCapable), definition);

        ToolCallback toolCallback = definition.create(decoratedChatClientFactory);

        return callbackDecorator.apply(withAskRelay(toolCallback, questionRenderer, askCapable), definition);
    }

    private ChatClient withAskTool(ChatClient chatClient, boolean askCapable) {
        if (askRelay == null || !askCapable) {
            return chatClient;
        }

        return chatClient.mutate()
            .defaultTools(askRelay.askUserQuestionToolCallback())
            .build();
    }

    private ToolCallback withAskRelay(
        ToolCallback toolCallback, SubAgentQuestionRenderer questionRenderer, boolean askCapable) {

        if (askRelay == null || !askCapable) {
            return toolCallback;
        }

        return new SubAgentAskRelayToolCallback(toolCallback, askRelay, questionRenderer);
    }
}

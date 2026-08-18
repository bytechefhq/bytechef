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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Central registry of the intelligent delegate tools — LLM-backed subagents exposed as {@link ToolCallback}s —
 * assembled from every {@link IntelligentToolContributor} bean on the classpath.
 *
 * <p>
 * Design rule: the catalog owns construction and identity; decoration belongs to the surface; nothing outside an
 * {@link IntelligentToolContributor} may construct an intelligent delegate {@link ToolCallback}.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class IntelligentToolCatalog {

    private final List<IntelligentToolDefinition> definitions;

    public IntelligentToolCatalog(ObjectProvider<IntelligentToolContributor> contributors) {
        this.definitions = contributors.orderedStream()
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .toList();
    }

    /**
     * Definitions whose name is in the given set — each management-MCP contributor owns a name partition, so this lets
     * it filter the catalog down to the names it owns today without double-registering another contributor's delegates.
     * The AI Hub also uses this, with its own owned name set, to reach every delegate it surfaces.
     */
    public List<ToolCallback> getByNames(
        Set<String> names,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        List<IntelligentToolDefinition> matchingDefinitions = definitions.stream()
            .filter(definition -> names.contains(definition.name()))
            .toList();

        return buildToolCallbacks(matchingDefinitions, variant, chatClientDecorator, callbackDecorator);
    }

    /**
     * Definitions whose {@link IntelligentToolDefinition#panelScopes()} contains the given scope — for Copilot panels.
     */
    public List<ToolCallback> getForPanel(
        IntelligentToolScope scope,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        List<IntelligentToolDefinition> matchingDefinitions = definitions.stream()
            .filter(definition -> definition.panelScopes()
                .contains(scope))
            .toList();

        return buildToolCallbacks(matchingDefinitions, variant, chatClientDecorator, callbackDecorator);
    }

    /**
     * Every <em>contributed</em> definition name, in contribution order — for the parity test and registration audits.
     * This reports names regardless of whether their {@link ChatClient} bean is actually available: a definition whose
     * {@link IntelligentToolDefinition#chatClientFactory(IntelligentToolVariant)} resolves to {@code null} for every
     * variant (e.g. copilot disabled) still appears here. Callers that need only the callbacks that will actually build
     * must go through {@link #getByNames} or {@link #getForPanel} instead.
     */
    public List<String> getNames() {
        return definitions.stream()
            .map(IntelligentToolDefinition::name)
            .toList();
    }

    private List<ToolCallback> buildToolCallbacks(
        List<IntelligentToolDefinition> candidateDefinitions,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        return candidateDefinitions.stream()
            .map(definition -> buildToolCallback(definition, variant, chatClientDecorator, callbackDecorator))
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Resolves the raw {@link IntelligentToolChatClientFactory} and wraps it so the per-surface
     * {@code chatClientDecorator} runs per delegation rather than per registration — the factory is not invoked here,
     * only composed, so nothing about a definition's {@link ChatClient} is built until a caller actually delegates to
     * it. This is what keeps guardrails, the workspace system prompt, and session memory attached to the client even
     * after it has been re-targeted at a caller-picked {@link org.springframework.ai.chat.model.ChatModel}.
     */
    @Nullable
    private ToolCallback buildToolCallback(
        IntelligentToolDefinition definition,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        IntelligentToolChatClientFactory rawChatClientFactory = definition.chatClientFactory(variant);

        if (rawChatClientFactory == null) {
            return null;
        }

        IntelligentToolChatClientFactory decoratedChatClientFactory =
            chatModel -> chatClientDecorator.apply(rawChatClientFactory.get(chatModel), definition);

        ToolCallback toolCallback = definition.create(decoratedChatClientFactory);

        return callbackDecorator.apply(toolCallback, definition);
    }
}

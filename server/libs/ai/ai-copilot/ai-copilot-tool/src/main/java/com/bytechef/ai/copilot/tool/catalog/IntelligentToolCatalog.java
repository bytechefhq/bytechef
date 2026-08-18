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
 * Central registry of the intelligent delegate tools — LLM-backed subagents exposed as {@link ToolCallback}s —
 * assembled from every {@link IntelligentToolContributor} bean on the classpath.
 *
 * <p>
 * Design rule: the catalog owns construction and identity; decoration belongs to the surface; nothing outside an
 * {@link IntelligentToolContributor} may construct an intelligent delegate {@link ToolCallback}.
 * </p>
 *
 * <p>
 * <b>Interactive questions.</b> When a {@link SubAgentAskRelay} is present, every callback the catalog builds for a
 * definition that {@link IntelligentToolDefinition#askCapable() declares itself ask-capable} — which is all of them
 * unless a definition opts out — is made ask-capable here and nowhere else: the specialist's own {@code ChatClient}
 * gains the relay's {@code askUserQuestion} tool, and the resulting {@link ToolCallback} is wrapped in a
 * {@link SubAgentAskRelayToolCallback} that carries a raised question out as the delegate's own tool result. Doing it
 * at this one seam is the point — every intelligent delegate gains the capability from one edit, and one added later
 * inherits it rather than having to remember. Absent the relay bean (a deployment without the EE module) nothing is
 * attached and nothing is wrapped, which is exactly the pre-existing behaviour.
 * </p>
 *
 * <p>
 * The opt-out is deliberately per definition rather than a name-keyed set kept here: a definition whose own prompt
 * forbids asking (the converter delegate, {@code importWorkflow}) must not be handed a tool whose description tells it
 * to ask, and a flag on the definition cannot drift away from the thing it gates.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
public class IntelligentToolCatalog {

    private final List<IntelligentToolDefinition> definitions;
    private final @Nullable SubAgentAskRelay askRelay;

    /**
     * The no-ask-relay shape: the catalog behaves exactly as it did before interactive questions existed. Kept as a
     * convenience for the many test fixtures that assemble a catalog to check narrowing or decoration and have no
     * interest in the ask capability.
     */
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

        return getByNames(names, variant, chatClientDecorator, callbackDecorator, SubAgentQuestionRenderer.JSON);
    }

    /**
     * As {@link #getByNames(Set, IntelligentToolVariant, BiFunction, BiFunction)}, choosing how a question a specialist
     * raises is rendered on this surface. Pass {@link SubAgentQuestionRenderer#PLAIN_TEXT} only where no client-side
     * renderer for the {@code ask-user-question} envelope exists — the management MCP surfaces. Panels and the AI Hub
     * must keep the default {@link SubAgentQuestionRenderer#JSON}: their client renders that envelope as a choice card,
     * and the contract is load-bearing.
     */
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

        return buildToolCallbacks(
            matchingDefinitions, variant, chatClientDecorator, callbackDecorator, SubAgentQuestionRenderer.JSON);
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
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator,
        SubAgentQuestionRenderer questionRenderer) {

        return candidateDefinitions.stream()
            .map(
                definition -> buildToolCallback(
                    definition, variant, chatClientDecorator, callbackDecorator, questionRenderer))
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

    /**
     * Attaches the specialist-facing {@code askUserQuestion} tool innermost — before the per-surface
     * {@code chatClientDecorator} — so a surface that wraps the client (the AI Hub's guardrails/session-memory
     * decorator) still delegates to a client that carries it. {@code defaultTools} appends to what the delegate's own
     * configuration already registered; nothing it declares is replaced.
     */
    private ChatClient withAskTool(ChatClient chatClient, boolean askCapable) {
        if (askRelay == null || !askCapable) {
            return chatClient;
        }

        return chatClient.mutate()
            .defaultTools(askRelay.askUserQuestionToolCallback())
            .build();
    }

    /**
     * Wraps the delegate so a question raised during its delegation comes back as its own tool result. Sits INSIDE the
     * per-surface {@code callbackDecorator} (progress reporting on the chat surface, workspace scoping on the
     * management MCP surface), which therefore sees an unchanged {@code ToolDefinition} and passes the question through
     * untouched.
     */
    private ToolCallback withAskRelay(
        ToolCallback toolCallback, SubAgentQuestionRenderer questionRenderer, boolean askCapable) {

        if (askRelay == null || !askCapable) {
            return toolCallback;
        }

        return new SubAgentAskRelayToolCallback(toolCallback, askRelay, questionRenderer);
    }
}

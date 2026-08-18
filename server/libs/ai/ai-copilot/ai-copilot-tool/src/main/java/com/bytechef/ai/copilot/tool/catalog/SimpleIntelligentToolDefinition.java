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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * A plain, function-composed {@link IntelligentToolDefinition} — the single shared implementation for every
 * {@link IntelligentToolContributor}, so each activation profile only needs to supply the per-variant
 * {@link IntelligentToolChatClientFactory} lookup and the {@link ToolCallback} factory.
 *
 * @author Ivica Cardic
 */
public final class SimpleIntelligentToolDefinition implements IntelligentToolDefinition {

    private final String name;
    private final String agentTypeKey;
    private final Set<IntelligentToolScope> panelScopes;
    private final Function<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoryFunction;
    private final Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory;
    private final boolean askCapable;

    /**
     * The ask-capable shape — the one nearly every delegate wants. Equivalent to passing {@code true} to
     * {@link #SimpleIntelligentToolDefinition(String, String, Set, Function, Function, boolean)}.
     */
    public SimpleIntelligentToolDefinition(
        String name, String agentTypeKey, Set<IntelligentToolScope> panelScopes,
        Function<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoryFunction,
        Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory) {

        this(name, agentTypeKey, panelScopes, chatClientFactoryFunction, toolCallbackFactory, true);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SimpleIntelligentToolDefinition(
        String name, String agentTypeKey, Set<IntelligentToolScope> panelScopes,
        Function<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoryFunction,
        Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory, boolean askCapable) {

        this.name = name;
        this.agentTypeKey = agentTypeKey;
        this.panelScopes = panelScopes;
        this.chatClientFactoryFunction = chatClientFactoryFunction;
        this.toolCallbackFactory = toolCallbackFactory;
        this.askCapable = askCapable;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String agentTypeKey() {
        return agentTypeKey;
    }

    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<IntelligentToolScope> panelScopes() {
        return panelScopes;
    }

    @Override
    @Nullable
    public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
        return chatClientFactoryFunction.apply(variant);
    }

    @Override
    public boolean askCapable() {
        return askCapable;
    }

    @Override
    public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
        return toolCallbackFactory.apply(chatClientFactory);
    }
}

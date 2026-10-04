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

    public List<ToolCallback> getByNames(
        Set<String> names,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator) {

        return definitions.stream()
            .filter(definition -> names.contains(definition.name()))
            .map(definition -> buildToolCallback(definition, variant, chatClientDecorator, callbackDecorator))
            .filter(Objects::nonNull)
            .toList();
    }

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
            () -> chatClientDecorator.apply(rawChatClientFactory.get(), definition);

        ToolCallback toolCallback = definition.create(decoratedChatClientFactory);

        return callbackDecorator.apply(toolCallback, definition);
    }
}

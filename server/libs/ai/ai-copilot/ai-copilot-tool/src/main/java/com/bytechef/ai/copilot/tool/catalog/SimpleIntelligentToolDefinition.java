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
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public final class SimpleIntelligentToolDefinition implements IntelligentToolDefinition {

    private final String name;
    private final Function<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoryFunction;
    private final Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SimpleIntelligentToolDefinition(
        String name, Function<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoryFunction,
        Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory) {

        this.name = name;
        this.chatClientFactoryFunction = chatClientFactoryFunction;
        this.toolCallbackFactory = toolCallbackFactory;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @Nullable
    public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
        return chatClientFactoryFunction.apply(variant);
    }

    @Override
    public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
        return toolCallbackFactory.apply(chatClientFactory);
    }
}

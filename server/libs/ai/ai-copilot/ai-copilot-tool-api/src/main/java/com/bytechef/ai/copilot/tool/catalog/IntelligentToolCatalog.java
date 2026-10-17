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

import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public interface IntelligentToolCatalog {

    List<ToolCallback> getByNames(
        Set<String> names,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator);

    List<ToolCallback> getByNames(
        Set<String> names,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator,
        SubAgentQuestionRenderer questionRenderer);

    List<ToolCallback> getForPanel(
        IntelligentToolScope scope,
        IntelligentToolVariant variant,
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator,
        BiFunction<ToolCallback, IntelligentToolDefinition, ToolCallback> callbackDecorator);

    List<String> getNames();
}

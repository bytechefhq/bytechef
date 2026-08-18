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

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Resolves the {@link ChatModel} a caller picked (in the AI Hub composer or a Copilot panel toolbar) for a delegate
 * tool call, from the {@code AgentToolInvocationContext} the delegate forwards.
 *
 * @author Ivica Cardic
 */
public interface SubAgentChatModelResolver {

    /**
     * Resolves the {@link ChatModel} the caller picked, from the tool context a delegate forwards. Returns {@code null}
     * whenever no pick is present or it cannot be honoured — the caller then uses its default client. Never throws.
     *
     * @param toolContext the forwarded tool context map
     * @return the picked {@link ChatModel}, or {@code null} when there is no usable pick
     */
    @Nullable
    ChatModel resolve(Map<String, Object> toolContext);
}

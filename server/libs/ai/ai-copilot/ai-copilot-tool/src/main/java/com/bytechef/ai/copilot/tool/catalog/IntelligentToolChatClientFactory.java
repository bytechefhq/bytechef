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

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Builds a delegate's {@link ChatClient}, invoked per delegation rather than per registration, so the delegate can be
 * re-targeted at the {@link ChatModel} the caller picked.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface IntelligentToolChatClientFactory {

    /**
     * @param chatModel the model the caller picked, or {@code null} for the contributor's default client
     */
    ChatClient get(@Nullable ChatModel chatModel);
}

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

package com.bytechef.platform.component.definition.ai.agent;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Implemented by chat memory repositories that can read the first message of each conversation without loading whole
 * conversations.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ConversationFirstMessageReader {

    /**
     * Returns the first message of at most {@code limit} conversations, most recently active first, optionally
     * restricted to conversation ids containing {@code searchText} (case-insensitive).
     */
    List<ConversationFirstMessage> findFirstMessages(@Nullable String searchText, int limit);

    record ConversationFirstMessage(String conversationId, String text) {
    }
}

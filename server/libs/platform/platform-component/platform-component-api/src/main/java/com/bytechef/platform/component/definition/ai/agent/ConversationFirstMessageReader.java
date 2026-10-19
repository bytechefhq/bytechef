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
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ConversationFirstMessageReader {

    /**
     * Returns the first message of the most recently active conversations.
     *
     * @param searchText case-insensitive substring the conversation id must contain, or {@code null} for all
     * @param limit      the maximum number of conversations to return
     * @return the first message of each matching conversation, most recently active first
     */
    List<ConversationFirstMessage> findFirstMessages(@Nullable String searchText, int limit);

    record ConversationFirstMessage(String conversationId, String text) {
    }
}

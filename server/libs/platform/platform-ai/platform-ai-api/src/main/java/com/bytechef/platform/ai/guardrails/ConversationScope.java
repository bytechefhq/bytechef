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

package com.bytechef.platform.ai.guardrails;

import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * @author Ivica Cardic
 */
public final class ConversationScope {

    public static final String PLATFORM_ISSUED_KEY = "bytechef.guardrails.conversationPlatformIssued";

    public static final String USER_ID_KEY = "bytechef.guardrails.conversationUserId";

    private ConversationScope() {
    }

    public static Optional<Key> trustedKey(Map<String, ?> context, @Nullable Long workspaceId) {
        if (workspaceId == null || !Boolean.TRUE.equals(context.get(PLATFORM_ISSUED_KEY))) {
            return Optional.empty();
        }

        if (!(context.get(ChatMemory.CONVERSATION_ID) instanceof String conversationId) || conversationId.isBlank()) {
            return Optional.empty();
        }

        if (!(context.get(USER_ID_KEY) instanceof Number userId)) {
            return Optional.empty();
        }

        return Optional.of(new Key(workspaceId, userId.longValue(), conversationId));
    }

    public record Key(long workspaceId, long userId, String conversationId) {
    }
}

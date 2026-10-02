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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;

/**
 * @author Ivica Cardic
 */
class ConversationScopeTest {

    @Test
    void testATrustedKeyNeedsAllThreeOfIdMarkerAndUser() {
        assertThat(ConversationScope.trustedKey(contextOf("thread-1", true, 7L), 42L))
            .contains(new ConversationScope.Key(42L, 7L, "thread-1"));
    }

    @Test
    void testAnIdWithoutTheMarkerIsNotTrusted() {
        assertThat(ConversationScope.trustedKey(contextOf("support-queue", false, 7L), 42L))
            .isEmpty();
    }

    @Test
    void testAMarkerWithoutAnIdIsNotTrusted() {
        assertThat(ConversationScope.trustedKey(contextOf(null, true, 7L), 42L)).isEmpty();
    }

    @Test
    void testAMarkerWithoutAUserIdIsNotTrusted() {
        assertThat(ConversationScope.trustedKey(contextOf("thread-1", true, null), 42L)).isEmpty();
    }

    @Test
    void testNoWorkspaceIsNotTrusted() {
        assertThat(ConversationScope.trustedKey(contextOf("thread-1", true, 7L), null)).isEmpty();
    }

    @Test
    void testAForeignTypeUnderAKeyIsIgnoredRatherThanThrowing() {
        Map<String, Object> context = contextOf("thread-1", true, 7L);

        context.put(ConversationScope.USER_ID_KEY, "not a number");

        assertThat(ConversationScope.trustedKey(context, 42L)).isEmpty();
    }

    @Test
    void testAnIntegerUserIdIsTrustedAndNarrowedToLong() {
        Map<String, Object> context = contextOf("thread-1", true, null);

        context.put(ConversationScope.USER_ID_KEY, Integer.valueOf(7));

        assertThat(ConversationScope.trustedKey(context, 42L))
            .contains(new ConversationScope.Key(42L, 7L, "thread-1"));
    }

    private static Map<String, Object> contextOf(String conversationId, boolean platformIssued, Long userId) {
        Map<String, Object> context = new HashMap<>();

        if (conversationId != null) {
            context.put(ChatMemory.CONVERSATION_ID, conversationId);
        }

        if (platformIssued) {
            context.put(ConversationScope.PLATFORM_ISSUED_KEY, Boolean.TRUE);
        }

        if (userId != null) {
            context.put(ConversationScope.USER_ID_KEY, userId);
        }

        return context;
    }
}

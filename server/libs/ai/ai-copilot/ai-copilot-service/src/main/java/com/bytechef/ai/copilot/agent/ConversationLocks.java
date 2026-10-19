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

package com.bytechef.ai.copilot.agent;

/**
 * @author Ivica Cardic
 */
final class ConversationLocks {

    private static final int LOCK_STRIPE_COUNT = 64;

    private static final Object[] CONVERSATION_LOCKS = createConversationLocks();

    private ConversationLocks() {
    }

    static Object get(String conversationId) {
        return CONVERSATION_LOCKS[Math.floorMod(conversationId.hashCode(), LOCK_STRIPE_COUNT)];
    }

    private static Object[] createConversationLocks() {
        Object[] conversationLocks = new Object[LOCK_STRIPE_COUNT];

        for (int index = 0; index < LOCK_STRIPE_COUNT; index++) {
            conversationLocks[index] = new Object();
        }

        return conversationLocks;
    }
}

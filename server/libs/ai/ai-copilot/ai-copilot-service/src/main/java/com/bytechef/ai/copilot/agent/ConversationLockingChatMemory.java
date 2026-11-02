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

import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * @author Ivica Cardic
 */
final class ConversationLockingChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    ConversationLockingChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        synchronized (ConversationLocks.get(conversationId)) {
            delegate.add(conversationId, messages);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        synchronized (ConversationLocks.get(conversationId)) {
            return delegate.get(conversationId);
        }
    }

    @Override
    public void clear(String conversationId) {
        synchronized (ConversationLocks.get(conversationId)) {
            delegate.clear(conversationId);
        }
    }
}

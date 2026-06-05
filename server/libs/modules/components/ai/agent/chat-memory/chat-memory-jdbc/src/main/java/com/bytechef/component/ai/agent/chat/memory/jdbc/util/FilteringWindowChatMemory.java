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

package com.bytechef.component.ai.agent.chat.memory.jdbc.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;

/**
 * Wraps {@link MessageWindowChatMemory} and re-applies tool-call sequence filtering after the message window is
 * applied. The underlying {@link org.springframework.ai.chat.memory.ChatMemoryRepository} may already filter broken
 * sequences from stored data, but the window slice can still produce orphaned
 * {@link org.springframework.ai.chat.messages.ToolResponseMessage} objects when it cuts a valid
 * AssistantMessage+ToolResponseMessage pair. This wrapper ensures the filtered view is always consistent before it
 * reaches the LLM.
 *
 * @author ByteChef
 */
public class FilteringWindowChatMemory implements ChatMemory {

    private final MessageWindowChatMemory delegate;

    @SuppressFBWarnings("EI2")
    public FilteringWindowChatMemory(MessageWindowChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        delegate.add(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        List<Message> messages = delegate.get(conversationId);

        return OrderedJdbcChatMemoryRepository.filterBrokenToolCallSequences(messages);
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }
}

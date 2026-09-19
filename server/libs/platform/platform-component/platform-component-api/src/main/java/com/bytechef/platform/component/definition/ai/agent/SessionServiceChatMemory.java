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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;

/**
 * Read-only {@link ChatMemory} view of a session, so callers reading conversation history through
 * {@link ChatMemoryFunction.Result#chatMemory()} see the same active events the session memory advisor sends to the
 * model. The advisor owns writes, so {@link #add(String, List)} and {@link #clear(String)} are unsupported.
 *
 * @author Ivica Cardic
 */
public final class SessionServiceChatMemory implements ChatMemory {

    private final EventFilter eventFilter;
    private final SessionService sessionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SessionServiceChatMemory(SessionService sessionService, EventFilter eventFilter) {
        this.eventFilter = eventFilter.merge(EventFilter.active());
        this.sessionService = sessionService;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        throw new UnsupportedOperationException("Session chat memory is written by its advisor");
    }

    @Override
    public List<Message> get(String conversationId) {
        return sessionService.getEvents(conversationId, eventFilter)
            .stream()
            .map(SessionEvent::getMessage)
            .toList();
    }

    @Override
    public void clear(String conversationId) {
        throw new UnsupportedOperationException("Session chat memory is written by its advisor");
    }
}

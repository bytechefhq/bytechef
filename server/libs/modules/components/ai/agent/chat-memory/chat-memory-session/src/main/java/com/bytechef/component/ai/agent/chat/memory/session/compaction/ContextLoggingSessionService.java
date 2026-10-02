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

package com.bytechef.component.ai.agent.chat.memory.session.compaction;

import com.bytechef.component.definition.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;

/**
 * @author Ivica Cardic
 */
public final class ContextLoggingSessionService implements SessionService {

    private final Context context;
    private final SessionService delegate;

    @SuppressFBWarnings("EI2")
    public ContextLoggingSessionService(SessionService delegate, Context context) {
        this.context = Objects.requireNonNull(context, "context");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public Session create(CreateSessionRequest createSessionRequest) {
        return delegate.create(createSessionRequest);
    }

    @Override
    public @Nullable Session findById(String sessionId) {
        return delegate.findById(sessionId);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        return delegate.findByUserId(userId);
    }

    @Override
    public void delete(String sessionId) {
        delegate.delete(sessionId);
    }

    @Override
    public int deleteExpiredSessions(Instant before) {
        return delegate.deleteExpiredSessions(before);
    }

    @Override
    public void appendEvent(SessionEvent sessionEvent) {
        delegate.appendEvent(sessionEvent);
    }

    @Override
    public void appendMessage(String sessionId, Message message) {
        delegate.appendMessage(sessionId, message);
    }

    @Override
    public List<SessionEvent> getEvents(String sessionId, EventFilter eventFilter) {
        return delegate.getEvents(sessionId, eventFilter);
    }

    @Override
    public List<SessionEvent> getEvents(String sessionId) {
        return delegate.getEvents(sessionId);
    }

    @Override
    public List<SessionEvent> findEventsByUserId(String userId, EventFilter eventFilter) {
        return delegate.findEventsByUserId(userId, eventFilter);
    }

    @Override
    public List<Message> getMessages(String sessionId) {
        return delegate.getMessages(sessionId);
    }

    @Override
    public List<Message> getActiveMessages(String sessionId) {
        return delegate.getActiveMessages(sessionId);
    }

    @Override
    public CompactionResult compact(
        String sessionId, CompactionTrigger compactionTrigger, CompactionStrategy compactionStrategy) {

        try {
            return delegate.compact(sessionId, compactionTrigger, compactionStrategy);
        } catch (RuntimeException exception) {
            String message = String.format(
                "Compacting the history of conversation '%s' failed; the history was left unchanged and compaction " +
                    "will be retried after the next turn: %s",
                sessionId, exception.getMessage());

            context.log(log -> log.warn(message, exception));

            throw exception;
        }
    }
}

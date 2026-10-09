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

package com.bytechef.component.ai.agent.chat.memory.builtin.util;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.compaction.CompactionPlan;

/**
 * @author Ivica Cardic
 */
final class UnavailableSessionRepository implements SessionRepository {

    private final String reason;

    UnavailableSessionRepository(String reason) {
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
    }

    @Override
    public Session save(Session session) {
        throw new IllegalStateException(reason);
    }

    @Override
    public boolean saveIfAbsent(Session session) {
        throw new IllegalStateException(reason);
    }

    @Override
    public @Nullable Session findById(String sessionId) {
        throw new IllegalStateException(reason);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        throw new IllegalStateException(reason);
    }

    @Override
    public void delete(String sessionId) {
        throw new IllegalStateException(reason);
    }

    @Override
    public int deleteExpiredSessions(Instant before) {
        return 0;
    }

    @Override
    public void appendEvent(SessionEvent event) {
        throw new IllegalStateException(reason);
    }

    @Override
    public boolean applyCompaction(String sessionId, CompactionPlan plan, long expectedVersion) {
        throw new IllegalStateException(reason);
    }

    @Override
    public long getEventVersion(String sessionId) {
        throw new IllegalStateException(reason);
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        throw new IllegalStateException(reason);
    }

    @Override
    public List<SessionEvent> findEventsByUserId(String userId, EventFilter filter) {
        throw new IllegalStateException(reason);
    }
}

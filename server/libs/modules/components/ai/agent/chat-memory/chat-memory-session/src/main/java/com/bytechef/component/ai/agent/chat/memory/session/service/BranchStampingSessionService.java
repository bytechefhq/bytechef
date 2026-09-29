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

package com.bytechef.component.ai.agent.chat.memory.session.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;

/**
 * @author Marko Kriskovic
 */
public final class BranchStampingSessionService implements SessionService {

    private final @Nullable String branch;
    private final SessionService sessionService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BranchStampingSessionService(SessionService sessionService, @Nullable String branch) {
        this.branch = branch;
        this.sessionService = sessionService;
    }

    @Override
    public Session create(CreateSessionRequest request) {
        return sessionService.create(request);
    }

    @Override
    public @Nullable Session findById(String sessionId) {
        return sessionService.findById(sessionId);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        return sessionService.findByUserId(userId);
    }

    @Override
    public void delete(String sessionId) {
        sessionService.delete(sessionId);
    }

    @Override
    public int deleteExpiredSessions(Instant before) {
        return sessionService.deleteExpiredSessions(before);
    }

    @Override
    public void appendEvent(SessionEvent event) {
        if (branch == null || event.getBranch() != null || event.isSynthetic()) {
            sessionService.appendEvent(event);

            return;
        }

        sessionService.appendEvent(SessionEvent.builder()
            .id(event.getId())
            .sessionId(event.getSessionId())
            .timestamp(event.getTimestamp())
            .message(event.getMessage())
            .metadata(event.getMetadata())
            .archived(event.isArchived())
            .branch(branch)
            .build());
    }

    @Override
    public List<SessionEvent> getEvents(String sessionId, EventFilter filter) {
        return sessionService.getEvents(sessionId, filter);
    }

    @Override
    public CompactionResult compact(String sessionId, CompactionTrigger trigger, CompactionStrategy strategy) {
        return sessionService.compact(sessionId, trigger, strategy);
    }
}

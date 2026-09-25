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

package com.bytechef.component.ai.agent.chat.memory.builtin.session.util;

import com.bytechef.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;

/**
 * Tenant-routing {@link SessionRepository} that gives every tenant its own repository, so identical session ids in
 * different tenants never share events. Each tenant's repository is built from its tenant id on first use and kept for
 * the life of the process: for an in-memory backend the repository holds the sessions themselves and must not be
 * evicted.
 *
 * @author Ivica Cardic
 */
public final class TenantRoutingSessionRepository implements SessionRepository {

    private final Map<String, SessionRepository> repositories = new ConcurrentHashMap<>();
    private final Function<String, SessionRepository> repositoryFactory;

    public TenantRoutingSessionRepository(Function<String, SessionRepository> repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    @Override
    public Session save(Session session) {
        return resolve().save(session);
    }

    @Override
    @Nullable
    public Session findById(String sessionId) {
        return resolve().findById(sessionId);
    }

    @Override
    public List<Session> findByUserId(String userId) {
        return resolve().findByUserId(userId);
    }

    @Override
    public List<String> findExpiredSessionIds(Instant before) {
        return resolve().findExpiredSessionIds(before);
    }

    @Override
    public void delete(String sessionId) {
        resolve().delete(sessionId);
    }

    @Override
    public void appendEvent(SessionEvent event) {
        resolve().appendEvent(event);
    }

    @Override
    public boolean compactEvents(
        String sessionId, List<SessionEvent> archivedEvents, List<SessionEvent> retainedEvents,
        long expectedVersion) {

        return resolve().compactEvents(sessionId, archivedEvents, retainedEvents, expectedVersion);
    }

    @Override
    public long getEventVersion(String sessionId) {
        return resolve().getEventVersion(sessionId);
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        return resolve().findEvents(sessionId, filter);
    }

    private SessionRepository resolve() {
        return repositories.computeIfAbsent(TenantContext.getCurrentTenantId(), repositoryFactory);
    }
}

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

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.compaction.CompactionPlan;

/**
 * @author Ivica Cardic
 */
public final class TenantRoutingSessionRepository implements SessionRepository {

    private final ConcurrentMap<String, SessionRepository> repositories;
    private final Function<String, SessionRepository> repositoryFactory;

    public TenantRoutingSessionRepository(Function<String, SessionRepository> repositoryFactory) {
        this(new ConcurrentHashMap<>(), repositoryFactory);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TenantRoutingSessionRepository(
        ConcurrentMap<String, SessionRepository> repositories, Function<String, SessionRepository> repositoryFactory) {

        this.repositories = repositories;
        this.repositoryFactory = repositoryFactory;
    }

    public int deleteExpiredSessionsOfLoadedTenants(Instant before) {
        int deletedCount = 0;

        for (Map.Entry<String, SessionRepository> entry : repositories.entrySet()) {
            SessionRepository sessionRepository = entry.getValue();

            deletedCount += TenantContext.callWithTenantId(
                entry.getKey(), () -> sessionRepository.deleteExpiredSessions(before));
        }

        return deletedCount;
    }

    @Override
    public Session save(Session session) {
        return resolve().save(session);
    }

    @Override
    public boolean saveIfAbsent(Session session) {
        return resolve().saveIfAbsent(session);
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
    public int deleteExpiredSessions(Instant before) {
        return resolve().deleteExpiredSessions(before);
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
    public boolean applyCompaction(String sessionId, CompactionPlan plan, long expectedVersion) {
        return resolve().applyCompaction(sessionId, plan, expectedVersion);
    }

    @Override
    public long getEventVersion(String sessionId) {
        return resolve().getEventVersion(sessionId);
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        return resolve().findEvents(sessionId, filter);
    }

    @Override
    public List<SessionEvent> findEventsByUserId(String userId, EventFilter filter) {
        return resolve().findEventsByUserId(userId, filter);
    }

    private SessionRepository resolve() {
        return repositories.computeIfAbsent(TenantContext.getCurrentTenantId(), repositoryFactory);
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.compaction.CompactionPlan;

/**
 * @author Ivica Cardic
 */
public final class TenantRoutingSessionRepository implements SessionRepository {

    private static final Logger log = LoggerFactory.getLogger(TenantRoutingSessionRepository.class);

    private final ConcurrentMap<String, SessionRepository> repositories;
    private final boolean repositoriesRetainEveryTenant;
    private final Function<String, SessionRepository> repositoryFactory;

    public TenantRoutingSessionRepository(Function<String, SessionRepository> repositoryFactory) {
        this(new ConcurrentHashMap<>(), true, repositoryFactory);
    }

    public TenantRoutingSessionRepository(
        ConcurrentMap<String, SessionRepository> repositories, Function<String, SessionRepository> repositoryFactory) {

        this(repositories, false, repositoryFactory);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private TenantRoutingSessionRepository(
        ConcurrentMap<String, SessionRepository> repositories, boolean repositoriesRetainEveryTenant,
        Function<String, SessionRepository> repositoryFactory) {

        this.repositories = Objects.requireNonNull(repositories, "repositories must not be null");
        this.repositoriesRetainEveryTenant = repositoriesRetainEveryTenant;
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "repositoryFactory must not be null");
    }

    public Map<String, Integer> deleteExpiredSessionsOfEveryTenant(Instant before) {
        if (!repositoriesRetainEveryTenant) {
            throw new IllegalStateException(
                "deleteExpiredSessionsOfEveryTenant requires the repositories map this class creates itself; an " +
                    "externally supplied map may have evicted tenants whose expired sessions would be skipped");
        }

        Map<String, Integer> deletedCounts = new HashMap<>();

        for (Map.Entry<String, SessionRepository> entry : repositories.entrySet()) {
            String tenantId = entry.getKey();
            SessionRepository sessionRepository = entry.getValue();

            try {
                int deletedCount = TenantContext.callWithTenantId(
                    tenantId, () -> sessionRepository.deleteExpiredSessions(before));

                deletedCounts.put(tenantId, deletedCount);
            } catch (RuntimeException runtimeException) {
                log.error("Failed to delete expired sessions for tenant {}", tenantId, runtimeException);
            }
        }

        return deletedCounts;
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

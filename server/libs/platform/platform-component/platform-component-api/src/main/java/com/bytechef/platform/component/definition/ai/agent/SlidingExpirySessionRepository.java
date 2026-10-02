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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.compaction.CompactionPlan;

/**
 * @author Ivica Cardic
 */
public final class SlidingExpirySessionRepository implements SessionRepository {

    private static final Duration MAX_REFRESH_GRANULARITY = Duration.ofDays(1);

    private final Clock clock;
    private final SessionRepository delegate;
    private final Duration refreshGranularity;
    private final Duration timeToLive;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SlidingExpirySessionRepository(SessionRepository delegate, Duration timeToLive, Clock clock) {
        Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(timeToLive, "timeToLive must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        if (timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("timeToLive must be positive but was " + timeToLive);
        }

        Duration halfTimeToLive = timeToLive.dividedBy(2);

        this.clock = clock;
        this.delegate = delegate;
        this.refreshGranularity = halfTimeToLive.compareTo(MAX_REFRESH_GRANULARITY) < 0
            ? halfTimeToLive : MAX_REFRESH_GRANULARITY;
        this.timeToLive = timeToLive;
    }

    @Override
    public Session save(Session session) {
        return delegate.save(withExpiresAt(session, getNextExpiresAt()));
    }

    @Override
    public boolean saveIfAbsent(Session session) {
        return delegate.saveIfAbsent(withExpiresAt(session, getNextExpiresAt()));
    }

    @Override
    @Nullable
    public Session findById(String sessionId) {
        Session session = delegate.findById(sessionId);

        if (session == null) {
            return null;
        }

        Instant expiresAt = session.expiresAt();

        if (expiresAt == null) {
            return session;
        }

        Instant now = clock.instant();

        if (expiresAt.isBefore(now)) {
            delegate.delete(sessionId);

            return null;
        }

        Instant nextExpiresAt = now.plus(timeToLive);

        if (!expiresAt.isBefore(nextExpiresAt.minus(refreshGranularity))) {
            return session;
        }

        return delegate.save(withExpiresAt(session, nextExpiresAt));
    }

    @Override
    public List<Session> findByUserId(String userId) {
        Instant now = clock.instant();

        return delegate.findByUserId(userId)
            .stream()
            .filter(session -> !isExpired(session, now))
            .toList();
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
    public void appendEvent(SessionEvent event) {
        delegate.appendEvent(event);
    }

    @Override
    public boolean applyCompaction(String sessionId, CompactionPlan plan, long expectedVersion) {
        return delegate.applyCompaction(sessionId, plan, expectedVersion);
    }

    @Override
    public long getEventVersion(String sessionId) {
        return delegate.getEventVersion(sessionId);
    }

    @Override
    public List<SessionEvent> findEvents(String sessionId, EventFilter filter) {
        return delegate.findEvents(sessionId, filter);
    }

    @Override
    public List<SessionEvent> findEventsByUserId(String userId, EventFilter filter) {
        Instant now = clock.instant();

        Set<String> expiredSessionIds = delegate.findByUserId(userId)
            .stream()
            .filter(session -> isExpired(session, now))
            .map(Session::id)
            .collect(Collectors.toSet());

        if (expiredSessionIds.isEmpty()) {
            return delegate.findEventsByUserId(userId, filter);
        }

        List<SessionEvent> liveSessionEvents = delegate.findEventsByUserId(userId, filter.withoutWindow())
            .stream()
            .filter(sessionEvent -> !expiredSessionIds.contains(sessionEvent.getSessionId()))
            .toList();

        return filter.applyWindow(liveSessionEvents);
    }

    private Instant getNextExpiresAt() {
        Instant now = clock.instant();

        return now.plus(timeToLive);
    }

    private static boolean isExpired(Session session, Instant now) {
        Instant expiresAt = session.expiresAt();

        return expiresAt != null && expiresAt.isBefore(now);
    }

    private static Session withExpiresAt(Session session, Instant expiresAt) {
        return Session.builder()
            .id(session.id())
            .userId(session.userId())
            .createdAt(session.createdAt())
            .expiresAt(expiresAt)
            .metadata(session.metadata())
            .build();
    }
}

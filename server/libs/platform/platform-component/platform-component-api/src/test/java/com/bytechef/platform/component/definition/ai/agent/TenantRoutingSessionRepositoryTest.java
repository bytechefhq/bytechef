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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
class TenantRoutingSessionRepositoryTest {

    @Test
    void testDeleteExpiredSessionsOfEveryTenantCoversEveryTenant() {
        TenantRoutingSessionRepository sessionRepository = createInMemorySessionRepository();

        Instant now = Instant.now();

        TenantContext.runWithTenantId("tenanta", () -> saveSessions(sessionRepository, now));
        TenantContext.runWithTenantId("tenantb", () -> saveSessions(sessionRepository, now));

        Map<String, Integer> deletedCounts = sessionRepository.deleteExpiredSessionsOfEveryTenant(now);

        assertThat(deletedCounts).containsOnly(entry("tenanta", 1), entry("tenantb", 1));
        assertThat(findSession(sessionRepository, "tenanta", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenantb", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNotNull();
    }

    @Test
    void testDeleteExpiredSessionsOfEveryTenantRunsInTheTenantContext() {
        List<String> tenantIds = new ArrayList<>();

        TenantRoutingSessionRepository sessionRepository = new TenantRoutingSessionRepository(
            tenantId -> createTenantRecordingSessionRepository(tenantIds));

        TenantContext.runWithTenantId("tenanta", () -> sessionRepository.findById("session-1"));
        TenantContext.runWithTenantId("tenantb", () -> sessionRepository.findById("session-1"));

        sessionRepository.deleteExpiredSessionsOfEveryTenant(Instant.now());

        assertThat(tenantIds).containsExactlyInAnyOrder("tenanta", "tenantb");
    }

    @Test
    void testDeleteExpiredSessionsOfEveryTenantContinuesPastAFailingTenant() {
        TenantRoutingSessionRepository sessionRepository = new TenantRoutingSessionRepository(
            tenantId -> "tenanta".equals(tenantId) ? createFailingSessionRepository() : InMemorySessionRepository
                .builder()
                .build());

        Instant now = Instant.now();

        TenantContext.runWithTenantId("tenanta", () -> sessionRepository.findById("session-1"));
        TenantContext.runWithTenantId("tenantb", () -> saveSessions(sessionRepository, now));

        Map<String, Integer> deletedCounts = sessionRepository.deleteExpiredSessionsOfEveryTenant(now);

        assertThat(deletedCounts).containsOnly(entry("tenantb", 1));
        assertThat(findSession(sessionRepository, "tenantb", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNotNull();
    }

    @Test
    void testDeleteExpiredSessionsOfEveryTenantRejectsAnExternallySuppliedRepositoryMap() {
        ConcurrentMap<String, SessionRepository> repositories = new ConcurrentHashMap<>();

        TenantRoutingSessionRepository sessionRepository = new TenantRoutingSessionRepository(
            repositories, tenantId -> InMemorySessionRepository.builder()
                .build());

        Instant now = Instant.now();

        assertThatThrownBy(() -> sessionRepository.deleteExpiredSessionsOfEveryTenant(now))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testConstructorRejectsNullArguments() {
        ConcurrentMap<String, SessionRepository> repositories = new ConcurrentHashMap<>();

        assertThatThrownBy(() -> new TenantRoutingSessionRepository(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("repositoryFactory");
        assertThatThrownBy(() -> new TenantRoutingSessionRepository(repositories, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("repositoryFactory");
        assertThatThrownBy(() -> new TenantRoutingSessionRepository(null, tenantId -> null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("repositories");
    }

    @Test
    void testSessionsAreIsolatedPerTenant() {
        TenantRoutingSessionRepository sessionRepository = createInMemorySessionRepository();

        Instant now = Instant.now();

        TenantContext.runWithTenantId("tenanta", () -> saveSessions(sessionRepository, now));

        assertThat(findSession(sessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNull();
    }

    @Test
    void testSameTenantReusesTheCachedRepository() {
        ConcurrentMap<String, SessionRepository> repositories = new ConcurrentHashMap<>();

        TenantRoutingSessionRepository sessionRepository = new TenantRoutingSessionRepository(
            repositories, tenantId -> InMemorySessionRepository.builder()
                .build());

        TenantContext.runWithTenantId("tenanta", () -> sessionRepository.findById("session-1"));

        SessionRepository firstSessionRepository = repositories.get("tenanta");

        TenantContext.runWithTenantId("tenanta", () -> sessionRepository.findById("session-1"));
        TenantContext.runWithTenantId("tenantb", () -> sessionRepository.findById("session-1"));

        assertThat(repositories.get("tenanta")).isSameAs(firstSessionRepository);
        assertThat(repositories.get("tenantb")).isNotSameAs(firstSessionRepository);
    }

    private static SessionRepository createFailingSessionRepository() {
        SessionRepository sessionRepository = mock(SessionRepository.class);

        when(sessionRepository.deleteExpiredSessions(any(Instant.class)))
            .thenThrow(new IllegalStateException("storage unavailable"));

        return sessionRepository;
    }

    private static SessionRepository createTenantRecordingSessionRepository(List<String> tenantIds) {
        SessionRepository sessionRepository = mock(SessionRepository.class);

        when(sessionRepository.deleteExpiredSessions(any(Instant.class)))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return 0;
            });

        return sessionRepository;
    }

    private static TenantRoutingSessionRepository createInMemorySessionRepository() {
        return new TenantRoutingSessionRepository(tenantId -> InMemorySessionRepository.builder()
            .build());
    }

    private static @Nullable Session findSession(
        SessionRepository sessionRepository, String tenantId, String sessionId) {

        return TenantContext.callWithTenantId(tenantId, () -> sessionRepository.findById(sessionId));
    }

    private static void saveSessions(SessionRepository sessionRepository, Instant now) {
        sessionRepository.save(Session.builder()
            .id("expired-session")
            .userId("user-1")
            .createdAt(now.minus(Duration.ofDays(2)))
            .expiresAt(now.minus(Duration.ofDays(1)))
            .build());
        sessionRepository.save(Session.builder()
            .id("live-session")
            .userId("user-1")
            .createdAt(now)
            .expiresAt(now.plus(Duration.ofDays(1)))
            .build());
    }
}

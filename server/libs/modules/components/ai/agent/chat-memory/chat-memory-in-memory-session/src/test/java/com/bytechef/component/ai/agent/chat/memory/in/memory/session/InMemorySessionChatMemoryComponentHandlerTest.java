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

package com.bytechef.component.ai.agent.chat.memory.in.memory.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public class InMemorySessionChatMemoryComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals(
            "definition/in-memory-session-chat-memory_v1.json",
            new InMemorySessionChatMemoryComponentHandler().getDefinition());
    }

    @Test
    public void testDeleteExpiredSessionsCoversEveryTenant() throws Exception {
        InMemorySessionChatMemoryComponentHandler componentHandler = new InMemorySessionChatMemoryComponentHandler();

        SessionRepository sessionRepository = getSessionRepository(componentHandler);

        Instant now = Instant.now();

        TenantContext.runWithTenantId("tenanta", () -> saveSessions(sessionRepository, now));
        TenantContext.runWithTenantId("tenantb", () -> saveSessions(sessionRepository, now));

        componentHandler.deleteExpiredSessions();

        assertThat(findSession(sessionRepository, "tenanta", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenantb", "expired-session")).isNull();
        assertThat(findSession(sessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNotNull();
    }

    @Test
    public void testComponentHandlersDoNotShareSessions() throws Exception {
        SessionRepository firstSessionRepository = getSessionRepository(
            new InMemorySessionChatMemoryComponentHandler());
        SessionRepository secondSessionRepository = getSessionRepository(
            new InMemorySessionChatMemoryComponentHandler());

        TenantContext.runWithTenantId("tenanta", () -> saveSessions(firstSessionRepository, Instant.now()));

        assertThat(findSession(firstSessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(secondSessionRepository, "tenanta", "live-session")).isNull();
    }

    private static @Nullable Session findSession(
        SessionRepository sessionRepository, String tenantId, String sessionId) {

        return TenantContext.callWithTenantId(tenantId, () -> sessionRepository.findById(sessionId));
    }

    @SuppressWarnings("unchecked")
    private static SessionRepository getSessionRepository(
        InMemorySessionChatMemoryComponentHandler componentHandler) throws Exception {

        List<ClusterElementDefinition<?>> clusterElementDefinitions = componentHandler.getDefinition()
            .getClusterElements()
            .orElseThrow();

        ClusterElementDefinition<SessionRepositoryFunction> clusterElementDefinition =
            (ClusterElementDefinition<SessionRepositoryFunction>) clusterElementDefinitions.getFirst();

        SessionRepositoryFunction sessionRepositoryFunction = clusterElementDefinition.getElement();

        return sessionRepositoryFunction.apply(null, null, null, null);
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

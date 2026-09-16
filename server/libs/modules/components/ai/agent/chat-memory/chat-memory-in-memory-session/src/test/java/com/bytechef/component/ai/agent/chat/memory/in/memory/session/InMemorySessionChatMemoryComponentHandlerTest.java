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
    public void testSessionsAreIsolatedPerTenant() throws Exception {
        SessionRepository sessionRepository = getSessionRepository(new InMemorySessionChatMemoryComponentHandler());

        TenantContext.runWithTenantId("tenanta", () -> saveSession(sessionRepository, "live-session"));

        assertThat(findSession(sessionRepository, "tenanta", "live-session")).isNotNull();
        assertThat(findSession(sessionRepository, "tenantb", "live-session")).isNull();
    }

    @Test
    public void testComponentHandlersDoNotShareSessions() throws Exception {
        SessionRepository firstSessionRepository = getSessionRepository(
            new InMemorySessionChatMemoryComponentHandler());
        SessionRepository secondSessionRepository = getSessionRepository(
            new InMemorySessionChatMemoryComponentHandler());

        TenantContext.runWithTenantId("tenanta", () -> saveSession(firstSessionRepository, "live-session"));

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

    private static void saveSession(SessionRepository sessionRepository, String sessionId) {
        sessionRepository.saveIfAbsent(Session.builder()
            .id(sessionId)
            .userId("user-1")
            .build());
    }
}

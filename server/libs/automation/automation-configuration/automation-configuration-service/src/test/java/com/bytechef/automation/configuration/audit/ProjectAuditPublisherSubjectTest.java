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

package com.bytechef.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.audit.ProjectAuditSubjectResolver.AuditSubject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The agent sharing path publishes through {@code ProjectSharingFacade}, so its audit records are keyed on the hidden
 * {@code __AI_AGENT__} project. An auditor reading one saw a project id that resolves to nothing they had ever seen in
 * the product. These pin the seam that fixes that without inventing a second event for one question.
 *
 * @author Ivica Cardic
 */
class ProjectAuditPublisherSubjectTest {

    private static final long AGENT_PROJECT_ID = 88L;
    private static final long PLAIN_PROJECT_ID = 89L;

    private final List<AuditEvent> auditEvents = new ArrayList<>();
    private final ApplicationEventPublisher applicationEventPublisher =
        event -> auditEvents.add(((AuditApplicationEvent) event).getAuditEvent());

    @Test
    void testRecordsTheSubjectAResolverClaims() {
        publisher(agentResolver()).publish(ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, AGENT_PROJECT_ID);

        assertThat(data()).containsEntry("subjectType", "AiAgent")
            .containsEntry("subjectId", "12")
            .containsEntry("subjectName", "Support Bot")
            .as("the project row is still the one that changed, so its id stays on the record")
            .containsEntry("projectId", "88");
    }

    /**
     * The claim is per project, not per publisher: a resolver that answered for everything would keep the test above
     * green while mislabelling every ordinary project in the log.
     */
    @Test
    void testRecordsNoSubjectForAProjectNoResolverClaims() {
        publisher(agentResolver()).publish(ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, PLAIN_PROJECT_ID);

        assertThat(data()).doesNotContainKey("subjectType")
            .doesNotContainKey("subjectId")
            .doesNotContainKey("subjectName")
            .containsEntry("projectId", "89");
    }

    /**
     * An audit record that names the project alone is worth more than a failed business transaction, so a resolver that
     * throws is skipped rather than propagated. The second resolver still runs and is still recorded, so the assertion
     * cannot be satisfied by swallowing the whole subject lookup.
     */
    @Test
    void testAThrowingResolverNeitherFailsTheWriteNorStopsTheNextOne() {
        ProjectAuditSubjectResolver throwingResolver = projectId -> {
            throw new IllegalStateException("boom");
        };

        publisher(throwingResolver, agentResolver())
            .publish(ProjectAuditEvent.PROJECT_ACCESS_GRANTED, AGENT_PROJECT_ID, Map.of("targetUserId", 3L));

        assertThat(data()).containsEntry("subjectType", "AiAgent")
            .containsEntry("targetUserId", 3L);
    }

    /**
     * Caller-supplied data wins, matching the {@code putIfAbsent} the projectId itself is written with — a caller that
     * already knows the subject must not have it overwritten by a lookup.
     */
    @Test
    void testCallerSuppliedSubjectIsNotOverwritten() {
        publisher(agentResolver())
            .publish(
                ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, AGENT_PROJECT_ID, Map.of("subjectName", "Renamed"));

        assertThat(data()).containsEntry("subjectName", "Renamed")
            .containsEntry("subjectId", "12");
    }

    private Map<String, Object> data() {
        assertThat(auditEvents).hasSize(1);

        AuditEvent auditEvent = auditEvents.getFirst();

        return auditEvent.getData();
    }

    private ProjectAuditPublisher publisher(ProjectAuditSubjectResolver... projectAuditSubjectResolvers) {
        return new ProjectAuditPublisher(applicationEventPublisher, List.of(projectAuditSubjectResolvers));
    }

    private static ProjectAuditSubjectResolver agentResolver() {
        return projectId -> projectId == AGENT_PROJECT_ID
            ? Optional.of(new AuditSubject("AiAgent", 12L, "Support Bot"))
            : Optional.empty();
    }
}

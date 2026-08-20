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

package com.bytechef.automation.ai.agent.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.ai.agent.config.AutomationAiAgentIntTestConfiguration;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.audit.ProjectAuditEvent;
import com.bytechef.automation.configuration.audit.ProjectAuditPublisher;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * The end-to-end half of the audit subject: the real {@link ProjectAuditPublisher}, the real component-scanned
 * {@link AiAgentProjectAuditSubjectResolver}, and a real {@code ai_agent} row pointing at a real hidden project.
 *
 * <p>
 * The two unit tests either side of this one would both stay green if the resolver were never registered as a bean, or
 * if the publisher never consulted the list — which is the whole mechanism. This is the test that would not.
 *
 * <p>
 * {@code PROJECT_VISIBILITY_CHANGED} is published directly rather than through {@code AiAgentSharingFacade}: that
 * facade is EE and lives in a module this CE slice does not carry, and what is being pinned is what the audit record
 * says, not who is allowed to trigger it. {@code AiAgentSharingFacadeTest} pins the delegation that reaches this
 * publisher.
 *
 * @author Ivica Cardic
 */
@RecordApplicationEvents
@SpringBootTest(classes = AutomationAiAgentIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AiAgentProjectAuditSubjectIntTest {

    @Autowired
    private AiAgentRepository agentRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private ProjectAuditPublisher projectAuditPublisher;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Project agentProject;
    private Project plainProject;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        agentProject = projectRepository.save(
            Project.builder()
                .name("__AI_AGENT__" + UUID.randomUUID())
                .workspaceId(workspace.getId())
                .build());
        plainProject = projectRepository.save(
            Project.builder()
                .name("test-project")
                .workspaceId(workspace.getId())
                .build());

        AiAgent agent = new AiAgent();

        agent.setName("support-bot");
        agent.setProjectId(agentProject.getId());
        agent.setTitle("Support Bot");
        agent.setUuid(UUID.randomUUID());
        agent.setWorkspaceId(workspace.getId());

        agentRepository.save(agent);
    }

    @AfterEach
    void afterEach() {
        agentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testSharingAnAgentsHiddenProjectIsAuditedAgainstTheAgent() {
        projectAuditPublisher.publish(
            ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, agentProject.getId(), Map.of("toVisibility", "PRIVATE"));

        AiAgent agent = agentRepository.findByProjectId(agentProject.getId())
            .orElseThrow();

        assertThat(auditEventData()).containsEntry("subjectType", "AiAgent")
            .containsEntry("subjectId", String.valueOf(agent.getId()))
            .containsEntry("subjectName", "Support Bot")
            .containsEntry("projectId", String.valueOf(agentProject.getId()));
    }

    /**
     * The control: an ordinary project in the same workspace, published through the same publisher in the same context,
     * carries no subject at all. Without it a resolver that claimed every project would pass the test above.
     */
    @Test
    void testAnOrdinaryProjectIsAuditedWithNoSubject() {
        projectAuditPublisher.publish(ProjectAuditEvent.PROJECT_VISIBILITY_CHANGED, plainProject.getId());

        assertThat(auditEventData()).doesNotContainKey("subjectType")
            .containsEntry("projectId", String.valueOf(plainProject.getId()));
    }

    private Map<String, Object> auditEventData() {
        AuditApplicationEvent auditApplicationEvent = applicationEvents.stream(AuditApplicationEvent.class)
            .reduce((first, second) -> second)
            .orElseThrow();

        AuditEvent auditEvent = auditApplicationEvent.getAuditEvent();

        return auditEvent.getData();
    }
}

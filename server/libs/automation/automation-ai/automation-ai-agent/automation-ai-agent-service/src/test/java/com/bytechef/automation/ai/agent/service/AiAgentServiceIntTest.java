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

package com.bytechef.automation.ai.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.ai.agent.config.AutomationAiAgentIntTestConfiguration;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationAiAgentIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AiAgentServiceIntTest {

    @Autowired
    private AiAgentService agentService;

    @Autowired
    private AiAgentChannelService agentChannelService;

    @Autowired
    private AiAgentElementService agentElementService;

    @Autowired
    private AiAgentRepository agentRepository;

    @Autowired
    private AiAgentChannelRepository agentChannelRepository;

    @Autowired
    private AiAgentElementRepository agentElementRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Project project;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test-workspace"));

        project = createProject("test-project");
    }

    @AfterEach
    void afterEach() {
        agentElementRepository.deleteAll();
        agentChannelRepository.deleteAll();
        agentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testCreateAndFetchAgent() {
        AiAgent agent = new AiAgent();

        agent.setName("support-bot");
        agent.setTitle("Support Bot");
        agent.setProjectWorkflowUuid(UUID.randomUUID());
        agent.setUuid(UUID.randomUUID());

        AiAgent saved = agentService.create(agent);

        assertThat(saved.getId()).isNotNull();
        assertThat(agentService.getAgent(saved.getId())
            .getName()).isEqualTo("support-bot");
    }

    @Test
    void testFetchAgentReturnsEmptyForMissingId() {
        Optional<AiAgent> fetchedAgent = agentService.fetchAgent(Long.MAX_VALUE);

        assertThat(fetchedAgent).isEmpty();
    }

    @Test
    void testUpdateAgent() {
        AiAgent agent = agentService.create(getAgent("support-bot"));

        agent.setTitle("Updated Title");
        agent.setDescription("Updated description");

        AiAgent updated = agentService.update(agent);

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void testDeleteAgent() {
        AiAgent agent = agentService.create(getAgent("support-bot"));

        agentService.delete(agent.getId());

        assertThat(agentRepository.findById(agent.getId())).isEmpty();
    }

    @Test
    void testGetAgentsFiltersByWorkspace() {
        AiAgent workspaceAgent = getAgent("workspace-bot");

        workspaceAgent.setWorkspaceId(42L);

        agentService.create(workspaceAgent);
        agentService.create(getAgent("workspaceless-bot"));

        assertThat(agentService.getAgents(42L))
            .extracting(AiAgent::getName)
            .containsExactly("workspace-bot");

        assertThat(agentService.getAgents(null))
            .hasSize(2);
    }

    @Test
    void testGetSubAgentReferencingAgents() {
        AiAgent parentAgent = agentService.create(getAgent("parent-bot"));
        AiAgent subAgent = agentService.create(getAgent("sub-bot"));
        AiAgent unrelatedAgent = agentService.create(getAgent("unrelated-bot"));

        AiAgentElement subAgentElement = new AiAgentElement(parentAgent.getId(), AiAgentElement.KIND_SUB_AGENT);

        subAgentElement.setReferenceId(subAgent.getId());

        agentElementService.create(subAgentElement);

        List<AiAgent> referencingAgents = agentService.getSubAgentReferencingAgents(subAgent.getId());

        assertThat(referencingAgents)
            .extracting(AiAgent::getId)
            .containsExactly(parentAgent.getId());

        assertThat(agentService.getSubAgentReferencingAgents(unrelatedAgent.getId())).isEmpty();
    }

    @Test
    void testGetProjectAgentsReturnsEveryAgentOfTheProject() {
        AiAgent firstAgent = agentService.create(getAgent("first-agent"));
        AiAgent secondAgent = agentService.create(getAgent("second-agent"));

        assertThat(agentService.getProjectAgents(project.getId()))
            .extracting(AiAgent::getId)
            .containsExactlyInAnyOrder(firstAgent.getId(), secondAgent.getId());
    }

    @Test
    void testProjectMembershipResolvesThroughProjectWorkflow() {
        Project otherProject = createProject("other-project");

        AiAgent firstAgent = agentService.create(getAgent("first-agent"));
        AiAgent secondAgent = agentService.create(getAgent("second-agent"));
        AiAgent otherAgent = agentService.create(getAgent("other-agent", otherProject));

        projectWorkflowRepository.save(
            new ProjectWorkflow(project.getId(), 2, UUID.randomUUID()
                .toString(), firstAgent.getProjectWorkflowUuid()));
        projectWorkflowRepository.save(
            new ProjectWorkflow(project.getId(), 2, UUID.randomUUID()
                .toString(), secondAgent.getProjectWorkflowUuid()));

        assertThat(agentService.getProjectAgents(project.getId()))
            .extracting(AiAgent::getId)
            .containsExactly(firstAgent.getId(), secondAgent.getId());
        assertThat(agentService.getProjectAgents(otherProject.getId()))
            .extracting(AiAgent::getId)
            .containsExactly(otherAgent.getId());

        assertThat(agentService.getProjectId(firstAgent)).isEqualTo(project.getId());
        assertThat(agentService.getProjectId(secondAgent)).isEqualTo(project.getId());
        assertThat(agentService.getProjectId(otherAgent)).isEqualTo(otherProject.getId());

        assertThat(agentService.getProjectIds(List.of(firstAgent, secondAgent, otherAgent)))
            .isEqualTo(
                Map.of(
                    firstAgent.getId(), project.getId(), secondAgent.getId(), project.getId(), otherAgent.getId(),
                    otherProject.getId()));
    }

    @Test
    void testAgentWithoutProjectWorkflowHasNoProject() {
        AiAgent orphanAgent = new AiAgent();

        orphanAgent.setName("orphan-agent");
        orphanAgent.setTitle("orphan-agent");
        orphanAgent.setProjectWorkflowUuid(UUID.randomUUID());
        orphanAgent.setUuid(UUID.randomUUID());

        AiAgent savedOrphanAgent = agentService.create(orphanAgent);
        AiAgent agent = agentService.create(getAgent("with-project"));

        assertThatThrownBy(() -> agentService.getProjectId(savedOrphanAgent))
            .isInstanceOf(IllegalStateException.class);

        assertThat(agentService.getProjectIds(List.of(savedOrphanAgent, agent)))
            .isEqualTo(Map.of(agent.getId(), project.getId()));
        assertThat(agentService.getProjectIds(List.of())).isEmpty();
    }

    @Test
    void testUpdateKeepsProjectWorkflowUuid() {
        AiAgent agent = agentService.create(getAgent("keeps-uuid"));
        UUID projectWorkflowUuid = agent.getProjectWorkflowUuid();

        agent.setTitle("Renamed");

        AiAgent updatedAgent = agentService.update(agent);

        assertThat(updatedAgent.getProjectWorkflowUuid()).isEqualTo(projectWorkflowUuid);
    }

    @Test
    void testChannelsAndElementsCascadeOnDelete() {
        AiAgent agent = agentService.create(getAgent("support-bot"));

        AiAgentChannel channel = new AiAgentChannel(agent.getId(), "WEBHOOK");

        agentChannelService.create(channel);

        AiAgentElement element = new AiAgentElement(agent.getId(), "TOOL");

        agentElementService.create(element);

        assertThat(agentChannelService.getByAgentId(agent.getId())).hasSize(1);
        assertThat(agentElementService.getByAgentId(agent.getId())).hasSize(1);

        agentService.delete(agent.getId());

        assertThat(agentChannelRepository.findAllByAgentIdOrderByPositionAsc(agent.getId())).isEmpty();
        assertThat(agentElementRepository.findAllByAgentIdOrderByPositionAsc(agent.getId())).isEmpty();
    }

    private Project createProject(String name) {
        Project newProject = Project.builder()
            .description(name)
            .name(name)
            .workspaceId(workspace.getId())
            .build();

        return projectRepository.save(newProject);
    }

    private AiAgent getAgent(String name) {
        return getAgent(name, project);
    }

    private AiAgent getAgent(String name, Project agentProject) {
        UUID projectWorkflowUuid = UUID.randomUUID();

        projectWorkflowRepository.save(
            new ProjectWorkflow(agentProject.getId(), 1, UUID.randomUUID()
                .toString(), projectWorkflowUuid));

        AiAgent agent = new AiAgent();

        agent.setName(name);
        agent.setTitle(name);
        agent.setProjectWorkflowUuid(projectWorkflowUuid);
        agent.setUuid(UUID.randomUUID());

        return agent;
    }
}

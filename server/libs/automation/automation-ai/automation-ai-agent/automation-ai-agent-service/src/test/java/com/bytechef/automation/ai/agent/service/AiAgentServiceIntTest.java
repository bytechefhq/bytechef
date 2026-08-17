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

import com.bytechef.automation.ai.agent.config.AutomationAiAgentIntTestConfiguration;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for {@link AiAgentService}, {@link AiAgentChannelService}, and {@link AiAgentElementService}. Also
 * exercises the {@code 00000000000001_automation_agent_init.xml} Liquibase changelog: Testcontainers builds the schema
 * from the changelog, so any column/constraint mismatch surfaces here.
 *
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
    private WorkspaceRepository workspaceRepository;

    private Project project;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        project = Project.builder()
            .description("test-project")
            .name("test-project")
            .workspaceId(workspace.getId())
            .build();

        project = projectRepository.save(project);
    }

    @AfterEach
    void afterEach() {
        agentElementRepository.deleteAll();
        agentChannelRepository.deleteAll();
        agentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testCreateAndFetchAgent() {
        AiAgent agent = new AiAgent();

        agent.setName("support-bot");
        agent.setTitle("Support Bot");
        agent.setProjectId(project.getId());
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

    private AiAgent getAgent(String name) {
        AiAgent agent = new AiAgent();

        agent.setName(name);
        agent.setTitle(name);
        agent.setProjectId(project.getId());
        agent.setUuid(UUID.randomUUID());

        return agent;
    }
}

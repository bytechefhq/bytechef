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

package com.bytechef.automation.ai.agent.subflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.agent.config.AutomationAiAgentIntTestConfiguration;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.CallableAiAgentEntry;
import com.bytechef.platform.workflow.task.dispatcher.subflow.CallableAiAgentDataSource.ResolvedAiAgent;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationAiAgentIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class CallableAiAgentDataSourceIntTest {

    @Autowired
    private CallableAiAgentDataSource callableAgentDataSource;

    @Autowired
    private AiAgentFacade agentFacade;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private AiAgentRepository agentRepository;

    @Autowired
    private AiAgentService agentService;

    @Autowired
    private AiAgentChannelRepository agentChannelRepository;

    @Autowired
    private AiAgentElementRepository agentElementRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceFacade workspaceFacade;

    private Long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        workspaceId = workspace.getId();

        Mockito.reset(userService, workspaceFacade);
    }

    @AfterEach
    void afterEach() {
        agentElementRepository.deleteAll();
        agentChannelRepository.deleteAll();
        agentRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testGetCallableAgentsListsPublishedAgent() {
        AiAgent agent = publishedAgent("Support Bot");

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents(null);

        assertThat(entries).extracting(CallableAiAgentEntry::agentUuid)
            .contains(agent.getUuid()
                .toString());
        assertThat(entries).extracting(CallableAiAgentEntry::title)
            .contains("Support Bot");
    }

    @Test
    void testGetCallableAgentsExcludesUnpublishedAgent() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Draft Bot", null, workspaceId, null);

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents(null);

        assertThat(entries).extracting(CallableAiAgentEntry::agentUuid)
            .doesNotContain(
                agentDTO.agent()
                    .getUuid()
                    .toString());
    }

    @Test
    void testGetCallableAgentsFiltersBySearchTitle() {
        publishedAgent("Invoice Assistant");
        publishedAgent("Order Assistant");

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents("invoice");

        assertThat(entries).extracting(CallableAiAgentEntry::title)
            .containsExactly("Invoice Assistant");
    }

    @Test
    void testResolveAgentMapsToPublishedWorkflowUuid() {
        AiAgent agent = publishedAgent("Support Bot");

        Project project = projectRepository.findById(agentService.getProjectId(agent))
            .orElseThrow();

        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agentService.getProjectId(agent), publishedVersion.getVersion());

        assertThat(publishedWorkflowIds).hasSize(1);

        ProjectWorkflow publishedProjectWorkflow =
            projectWorkflowService.getWorkflowProjectWorkflow(publishedWorkflowIds.get(0));

        ResolvedAiAgent resolvedAgent = callableAgentDataSource.resolveAgent(
            agent.getUuid()
                .toString(),
            false);

        assertThat(resolvedAgent.workflowUuid()).isEqualTo(publishedProjectWorkflow.getUuidAsString());
        assertThat(resolvedAgent.name()).isEqualTo(agent.getName());
    }

    @Test
    void testResolveAgentThrowsClearErrorForUnpublishedAgent() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Draft Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(
            () -> callableAgentDataSource.resolveAgent(
                agent.getUuid()
                    .toString(),
                false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no published version");
    }

    @Test
    void testGetCallableAgentsExcludesAgentAddedAfterProjectPublish() {
        AiAgent publishedAgent = publishedAgent("Support Bot");

        AiAgent lateAgent = lateAgent(publishedAgent);

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents(null);

        assertThat(entries).extracting(CallableAiAgentEntry::agentUuid)
            .contains(
                publishedAgent.getUuid()
                    .toString())
            .doesNotContain(
                lateAgent.getUuid()
                    .toString());
    }

    @Test
    void testResolveAgentThrowsClearErrorForAgentAddedAfterProjectPublish() {
        AiAgent lateAgent = lateAgent(publishedAgent("Support Bot"));

        assertThatThrownBy(
            () -> callableAgentDataSource.resolveAgent(
                lateAgent.getUuid()
                    .toString(),
                false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no published version");
    }

    @Test
    void testResolveAgentThrowsClearErrorForUnknownUuid() {
        assertThatThrownBy(() -> callableAgentDataSource.resolveAgent("00000000-0000-0000-0000-000000000000", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void testResolveAgentInEditorEnvironmentAllowsUnpublishedAgent() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Draft Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        ResolvedAiAgent resolvedAgent = callableAgentDataSource.resolveAgent(
            agent.getUuid()
                .toString(),
            true);

        assertThat(resolvedAgent.workflowUuid()).isNotBlank();
    }

    @Test
    void testGetCallableAgentsExcludesAgentInInaccessibleWorkspace() {
        AiAgent agent = publishedAgent("Support Bot");

        stubAccessibleWorkspaces(workspaceId + 1_000_000L);

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents(null);

        assertThat(entries).extracting(CallableAiAgentEntry::agentUuid)
            .doesNotContain(
                agent.getUuid()
                    .toString());
    }

    @Test
    void testGetCallableAgentsIncludesAgentInAccessibleWorkspace() {
        AiAgent agent = publishedAgent("Support Bot");

        stubAccessibleWorkspaces(workspaceId);

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents(null);

        assertThat(entries).extracting(CallableAiAgentEntry::agentUuid)
            .contains(
                agent.getUuid()
                    .toString());
    }

    @Test
    void testGetCallableAgentsUnfilteredWhenNoPrincipal() {
        AiAgent agent = publishedAgent("Support Bot");

        when(userService.fetchCurrentUser()).thenReturn(Optional.empty());

        List<CallableAiAgentEntry> entries = callableAgentDataSource.getCallableAgents(null);

        assertThat(entries).extracting(CallableAiAgentEntry::agentUuid)
            .contains(
                agent.getUuid()
                    .toString());
    }

    @Test
    void testResolveAgentRejectsAgentInInaccessibleWorkspace() {
        AiAgent agent = publishedAgent("Support Bot");

        stubAccessibleWorkspaces(workspaceId + 1_000_000L);

        assertThatThrownBy(
            () -> callableAgentDataSource.resolveAgent(
                agent.getUuid()
                    .toString(),
                false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not accessible");
    }

    @Test
    void testResolveAgentAllowsAgentInAccessibleWorkspace() {
        AiAgent agent = publishedAgent("Support Bot");

        stubAccessibleWorkspaces(workspaceId);

        ResolvedAiAgent resolvedAgent = callableAgentDataSource.resolveAgent(
            agent.getUuid()
                .toString(),
            false);

        assertThat(resolvedAgent.workflowUuid()).isNotBlank();
    }

    private void stubAccessibleWorkspaces(Long... accessibleWorkspaceIds) {
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        List<Workspace> workspaces = List.of(accessibleWorkspaceIds)
            .stream()
            .map(accessibleWorkspaceId -> {
                Workspace workspace = mock(Workspace.class);

                when(workspace.getId()).thenReturn(accessibleWorkspaceId);

                return workspace;
            })
            .toList();

        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(workspaces);
    }

    private AiAgent lateAgent(AiAgent publishedAgent) {
        AiAgentDTO lateAgentDTO = agentFacade.createAgent(
            "Late Bot", "A late agent.", workspaceId, agentService.getProjectId(publishedAgent));

        return lateAgentDTO.agent();
    }

    private AiAgent publishedAgent(String title) {
        AiAgentDTO agentDTO = agentFacade.createAgent(title, "A helpful agent.", workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        publishProject(agent, "First release");

        return agent;
    }

    private int publishProject(AiAgent agent, String description) {
        long projectId = agentService.getProjectId(agent);

        int oldProjectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, oldProjectVersion);

        int newProjectVersion = projectService.publishProject(projectId, description, false);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.publishWorkflow(
                projectId, oldProjectVersion, oldWorkflowId, oldProjectWorkflow);
        }

        return newProjectVersion;
    }
}

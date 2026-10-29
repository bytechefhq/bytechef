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

package com.bytechef.automation.ai.agent.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.ai.agent.channel.AgentChannelResolver;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.service.AiAgentChannelService;
import com.bytechef.automation.ai.agent.service.AiAgentElementService;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.ai.agent.service.AiAgentVersionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiAgentFacadeDeploymentLookupTest {

    private static final String AGENT_WORKFLOW_ID = "agent-workflow";
    private static final long AGENT_ID = 10L;
    private static final long PROJECT_ID = 100L;
    private static final long WORKSPACE_ID = 1L;

    private final AiAgent agent = agent();
    private final AiAgentService agentService = mock(AiAgentService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private AiAgentFacadeImpl aiAgentFacade;

    @BeforeEach
    void setUp() {
        when(agentService.getAgent(AGENT_ID)).thenReturn(agent);
        when(agentService.getAgents(WORKSPACE_ID)).thenReturn(List.of(agent));
        when(agentService.getProjectId(agent)).thenReturn(PROJECT_ID);
        when(agentService.getProjectIds(anyCollection())).thenReturn(Map.of(AGENT_ID, PROJECT_ID));

        Project project = new Project();

        project.setId(PROJECT_ID);
        project.setWorkspaceId(WORKSPACE_ID);

        when(projectService.getProject(PROJECT_ID)).thenReturn(project);

        when(workflowService.getWorkflow(AGENT_WORKFLOW_ID))
            .thenReturn(new Workflow(AGENT_WORKFLOW_ID, "{\"label\":\"Agent\",\"tasks\":[]}", Workflow.Format.JSON));

        aiAgentFacade = new AiAgentFacadeImpl(
            agentService, mock(AgentChannelResolver.class), mock(AiAgentChannelService.class),
            mock(AiAgentElementService.class), mock(AiAgentVersionService.class), mock(EnvironmentService.class),
            projectService,
            projectWorkflowService, projectDeploymentService,
            projectDeploymentWorkflowService, mock(TriggerDefinitionService.class), workflowService,
            mock(WorkflowTestConfigurationService.class), mock(WorkspaceConnectionService.class),
            mock(WorkspaceKnowledgeBaseService.class),
            mock(PrincipalJobService.class), mock(JobService.class),
            mock(UserService.class), "http://localhost/webhooks/{id}", List.of());
    }

    @Test
    void testGetAgentDeploymentsListsEveryDeploymentInOneEnvironmentThatContainsTheAgent() {
        stubDeployments(
            deployment(1L, true, true, false), deployment(2L, true, true, true), deployment(3L, true, false, false));

        List<AiAgentDeploymentDTO> agentDeployments = aiAgentFacade.getAgentDeployments(WORKSPACE_ID);

        assertThat(agentDeployments)
            .as("one row per deployment carrying the agent's workflow; the unrelated deployment yields none")
            .extracting(AiAgentDeploymentDTO::id)
            .containsExactly(1L, 2L);
    }

    @Test
    void testDeleteAgentIsRefusedWhenEnabledInASecondDeploymentOfTheSameEnvironment() {
        stubDeployments(deployment(1L, true, true, false), deployment(2L, true, true, true));

        assertThatThrownBy(() -> aiAgentFacade.deleteAgent(AGENT_ID))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("enabled in a deployment");

        verify(agentService, never()).delete(AGENT_ID);
    }

    private void stubDeployments(DeploymentFixture... deploymentFixtures) {
        List<ProjectDeployment> projectDeployments = new ArrayList<>();

        for (DeploymentFixture deploymentFixture : deploymentFixtures) {
            ProjectDeployment projectDeployment = deploymentFixture.projectDeployment();

            projectDeployments.add(projectDeployment);

            long projectDeploymentId = projectDeployment.getId();

            when(projectWorkflowService.fetchProjectWorkflowWorkflowId(
                projectDeploymentId, String.valueOf(agent.getProjectWorkflowUuid())))
                    .thenReturn(
                        deploymentFixture.containsAgent() ? Optional.of(AGENT_WORKFLOW_ID) : Optional.empty());

            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setEnabled(deploymentFixture.agentWorkflowEnabled());
            projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
            projectDeploymentWorkflow.setWorkflowId(
                deploymentFixture.containsAgent() ? AGENT_WORKFLOW_ID : "unrelated-workflow");

            when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId))
                .thenReturn(List.of(projectDeploymentWorkflow));
        }

        when(projectDeploymentService.getAllProjectDeployments(PROJECT_ID)).thenReturn(projectDeployments);
    }

    private static AiAgent agent() {
        AiAgent agent = new AiAgent();

        agent.setId(AGENT_ID);
        agent.setName("support-bot");
        agent.setTitle("Support Bot");
        agent.setProjectWorkflowUuid(UUID.randomUUID());
        agent.setWorkspaceId(WORKSPACE_ID);

        return agent;
    }

    private static DeploymentFixture deployment(
        long id, boolean enabled, boolean containsAgent, boolean agentWorkflowEnabled) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnabled(enabled);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setId(id);
        projectDeployment.setName("Deployment " + id);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(1);

        return new DeploymentFixture(projectDeployment, containsAgent, agentWorkflowEnabled);
    }

    private record DeploymentFixture(
        ProjectDeployment projectDeployment, boolean containsAgent, boolean agentWorkflowEnabled) {
    }
}

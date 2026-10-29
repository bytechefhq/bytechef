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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.agent.channel.AiAgentChannelType;
import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.config.AutomationAiAgentIntTestConfiguration;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentVersionDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import com.bytechef.automation.ai.agent.exception.AiAgentErrorType;
import com.bytechef.automation.ai.agent.repository.AiAgentChannelRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentElementRepository;
import com.bytechef.automation.ai.agent.repository.AiAgentRepository;
import com.bytechef.automation.ai.agent.service.AiAgentService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.listener.ProjectDeleteEventListener;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
class AiAgentFacadeIntTest {

    private static final String CHAT_TRIGGER_NAME = "chat_1";

    private static final String SLACK_CHANNEL_TYPE = "slack";

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
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Autowired
    private List<ProjectDeleteEventListener> projectDeleteEventListeners;

    private Long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        Mockito.reset(triggerDefinitionService);

        agentElementRepository.deleteAll();
        agentChannelRepository.deleteAll();
        agentRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();

        for (Workflow workflow : workflowCrudRepository.findAll()) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testCreateAgentWithoutProjectProvisionsVisibleProjectNamedAfterAgent() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Handles support questions", workspaceId, null);

        AiAgent agent = agentDTO.agent();
        Project project = projectService.getProject(agentService.getProjectId(agent));

        assertThat(project.getName()).isEqualTo("Support Bot");
        assertThat(SystemProjects.isSystemProject(project)).isFalse();

        ProjectWorkflow projectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(
                project.getId(), project.getLastProjectVersion(), String.valueOf(agent.getProjectWorkflowUuid()))
            .orElseThrow();

        assertThat(projectWorkflow.getType()).isEqualTo(ProjectWorkflowType.AI_AGENT);

        String definition = draftDefinition(agent);

        assertThat(definition).contains("\"name\":\"chat_1\"");
        assertThat(definition).contains("\"name\":\"workflowCall_1\"");

        assertThat(agentDTO.channels()).extracting(AiAgentChannel::getChannelType)
            .containsExactlyInAnyOrder("chat", "workflowCall");
        assertThat(agentDTO.elements()).extracting(AiAgentElement::getKind)
            .containsExactly(AiAgentElement.KIND_CHAT_MEMORY);
        assertThat(agentDTO.unpublishedChanges()).isTrue();
        assertThat(agentDTO.lastPublishedVersion()).isZero();
    }

    @Test
    void testCreateAgentSuffixesProjectNameOnCollision() {
        agentFacade.createAgent("Support Bot", null, workspaceId, null);

        AiAgentDTO secondAgentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);

        Project secondProject = projectService.getProject(secondAgentDTO.projectId());

        assertThat(secondProject.getName()).isEqualTo("Support Bot (2)");
    }

    @Test
    void testGetAgentsReportsEachAgentsOwnProject() {
        AiAgentDTO firstAgentDTO = agentFacade.createAgent("First", null, workspaceId, null);

        long projectId = firstAgentDTO.projectId();

        AiAgentDTO secondAgentDTO = agentFacade.createAgent("Second", null, workspaceId, projectId);
        AiAgentDTO otherAgentDTO = agentFacade.createAgent("Other", null, workspaceId, null);

        long otherProjectId = otherAgentDTO.projectId();

        assertThat(otherProjectId).isNotEqualTo(projectId);

        Map<Long, Long> listedProjectIds = agentFacade.getAgents(workspaceId)
            .stream()
            .collect(Collectors.toMap(agentDTO -> agentDTO.agent()
                .getId(), AiAgentDTO::projectId));

        assertThat(listedProjectIds).isEqualTo(
            Map.of(
                firstAgentDTO.agent()
                    .getId(),
                projectId,
                secondAgentDTO.agent()
                    .getId(),
                projectId,
                otherAgentDTO.agent()
                    .getId(),
                otherProjectId));

        assertThat(agentFacade.getAgent(secondAgentDTO.agent()
            .getId())
            .projectId()).isEqualTo(projectId);
        assertThat(agentService.getProjectAgents(projectId))
            .extracting(AiAgent::getId)
            .containsExactlyInAnyOrder(firstAgentDTO.agent()
                .getId(),
                secondAgentDTO.agent()
                    .getId());
    }

    @Test
    void testTwoAgentsAndAWorkflowShareOneProject() {
        AiAgentDTO firstAgentDTO = agentFacade.createAgent("First", null, workspaceId, null);

        long projectId = firstAgentDTO.projectId();

        AiAgentDTO secondAgentDTO = agentFacade.createAgent("Second", null, workspaceId, projectId);

        Project project = projectService.getProject(projectId);

        Workflow userWorkflow = workflowService.create(
            "{\"label\":\"User workflow\",\"tasks\":[]}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(projectId, project.getLastProjectVersion(), userWorkflow.getId());

        assertThat(secondAgentDTO.projectId()).isEqualTo(projectId);
        assertThat(agentFacade.getDraftWorkflowId(firstAgentDTO.agent()
            .getId()))
                .isNotEqualTo(agentFacade.getDraftWorkflowId(secondAgentDTO.agent()
                    .getId()));

        agentFacade.updateAgent(
            firstAgentDTO.agent()
                .getId(),
            "First", null, "You are the first agent");

        assertThat(draftDefinition(firstAgentDTO.agent())).contains("You are the first agent");
        assertThat(draftDefinition(secondAgentDTO.agent())).doesNotContain("You are the first agent");
    }

    @Test
    void testCreateAgentRejectsProjectOfAnotherWorkspace() {
        Workspace otherWorkspace = workspaceRepository.save(new Workspace("other-workspace"));

        AiAgentDTO otherAgentDTO = agentFacade.createAgent("Other", null, otherWorkspace.getId(), null);

        long otherProjectId = otherAgentDTO.projectId();

        assertThatThrownBy(() -> agentFacade.createAgent("Intruder", null, workspaceId, otherProjectId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAgentAddedAfterProjectPublishReportsNeverPublished() {
        AiAgentDTO firstAgentDTO = createPublishableAgent("First");

        publishProject(firstAgentDTO.agent(), "v1");

        AiAgentDTO lateAgentDTO = agentFacade.createAgent(
            "Late", null, workspaceId, firstAgentDTO.projectId());

        assertThat(lateAgentDTO.lastPublishedVersion()).isZero();
        assertThat(lateAgentDTO.unpublishedChanges()).isTrue();
    }

    @Test
    void testAddModelElementRegeneratesDefinitionWithModelClusterElement() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        String definition = draftDefinition(agent);

        assertThat(definition).contains("\"openai/v1/model\"");
    }

    @Test
    void testAddModelElementWithConnectionIdSyncsWorkflowTestConfigurationConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        String workflowId = draftWorkflowId(agent);

        WorkflowTestConfiguration workflowTestConfiguration = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, Environment.DEVELOPMENT.ordinal())
            .orElseThrow();

        assertThat(workflowTestConfiguration.getConnections())
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(org.assertj.core.groups.Tuple.tuple("aiAgent_1", "openai_1", 42L));
    }

    @Test
    void testApprovalDeliveryReusesTheAgentChannelConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentChannel(agent.getId(), SLACK_CHANNEL_TYPE, Map.of(), 77L);

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_TOOL, null,
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendChannelMessage", "parameters",
                Map.of()),
            null);

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, Map.of(), null);

        assertThat(draftTestConnections(agent))
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(org.assertj.core.groups.Tuple.tuple("aiAgent_1", "slack_2", 77L));
    }

    @Test
    void testApprovalDeliveryKeepsTheModelConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);
        agentFacade.addAgentChannel(agent.getId(), SLACK_CHANNEL_TYPE, Map.of(), 77L);
        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, Map.of(), null);

        assertThat(draftTestConnections(agent))
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(
                org.assertj.core.groups.Tuple.tuple("aiAgent_1", "openai_1", 42L),
                org.assertj.core.groups.Tuple.tuple("aiAgent_1", "slack_1", 77L));
    }

    @Test
    void testClearedModelConnectionIsRemovedFromTheTestConnections() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentElement modelElement = agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        assertThat(draftTestConnections(agent)).extracting(WorkflowTestConfigurationConnection::getConnectionId)
            .contains(42L);

        agentFacade.deleteAgentElement(modelElement.getId());
        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        assertThat(draftTestConnections(agent)).extracting(WorkflowTestConfigurationConnection::getConnectionId)
            .doesNotContain(42L);
    }

    @Test
    void testChannelReplyTaskGetsTheChannelConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentChannel(agent.getId(), "telegram", Map.of(), 77L);

        assertThat(draftTestConnections(agent))
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(
                org.assertj.core.groups.Tuple.tuple("telegram_1", "telegram", 77L),
                org.assertj.core.groups.Tuple.tuple("reply_telegram_1", "telegram", 77L));
    }

    @Test
    void testConnectionsOutsideTheAgentWorkspaceAreRejected() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 1234L))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Connection 1234");

        assertThatThrownBy(() -> agentFacade.addAgentChannel(agent.getId(), SLACK_CHANNEL_TYPE, Map.of(), 1234L))
            .isInstanceOf(ConfigurationException.class);

        assertThatThrownBy(() -> agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 1234))))
                .isInstanceOf(ConfigurationException.class);

        assertThat(agentElementRepository.findAll())
            .noneMatch(element -> AiAgentElement.KIND_MODEL.equals(element.getKind()));
    }

    @Test
    void testReferencesOutsideTheAgentWorkspaceAreRejected() {
        AiAgent agent = agentFacade.createAgent("Support Bot", null, workspaceId, null)
            .agent();

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_KNOWLEDGE_BASE, 999L, Map.of(), null))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Knowledge base 999")
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AiAgentErrorType.REFERENCE_NOT_IN_WORKSPACE.getErrorKey()));

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_KNOWLEDGE_BASE, 300L, Map.of(), null);

        Workspace otherWorkspace = workspaceRepository.save(new Workspace("other-workspace"));

        AiAgent otherWorkspaceAgent = agentFacade.createAgent("Other Bot", null, otherWorkspace.getId(), null)
            .agent();

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(
                agent.getId(), AiAgentElement.KIND_SUB_AGENT, otherWorkspaceAgent.getId(), Map.of(), null))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(AiAgentErrorType.REFERENCE_NOT_IN_WORKSPACE.getErrorKey()));

        assertThat(agentFacade.getAgent(agent.getId())
            .elements()).extracting(AiAgentElement::getReferenceId)
                .contains(300L)
                .doesNotContain(999L, otherWorkspaceAgent.getId());
    }

    @Test
    void testAddSecondModelElementThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(
                agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(AiAgentErrorType.ELEMENT_KIND_ALREADY_PRESENT.getErrorKey()));
    }

    @Test
    void testDeleteChatChannelThrowsChannelNotDeletable() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);

        AiAgentChannel chatChannel = agentDTO.channels()
            .stream()
            .filter(channel -> "chat".equals(channel.getChannelType()))
            .findFirst()
            .orElseThrow();

        assertThatThrownBy(() -> agentFacade.deleteAgentChannel(chatChannel.getId()))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_NOT_DELETABLE.getErrorKey()));
    }

    @Test
    void testDeleteAgentWithSubAgentReferenceThrows() {
        AiAgentDTO parentAgentDTO = agentFacade.createAgent("Parent Bot", null, workspaceId, null);
        AiAgentDTO subAgentDTO = agentFacade.createAgent("Sub Bot", null, workspaceId, null);

        AiAgent parentAgent = parentAgentDTO.agent();
        AiAgent subAgent = subAgentDTO.agent();

        agentFacade.addAgentElement(
            parentAgent.getId(), AiAgentElement.KIND_SUB_AGENT, subAgent.getId(), null, null);

        assertThatThrownBy(() -> agentFacade.deleteAgent(subAgent.getId()))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.AGENT_REFERENCED_AS_SUB_AGENT.getErrorKey()));
    }

    @Test
    void testDeleteAgentEnabledInDeploymentThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        deploy(agentService.getProjectId(agent), draftWorkflowId(agent), true, true);

        assertThatThrownBy(() -> agentFacade.deleteAgent(agent.getId()))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.AGENT_HAS_DEPLOYMENTS.getErrorKey()));
    }

    @Test
    void testDeleteAgentWhoseProjectIsDeployedWithoutItSucceeds() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        long projectId = agentService.getProjectId(agent);

        Project project = projectService.getProject(projectId);

        Workflow userWorkflow = workflowService.create(
            "{\"label\":\"Deployed\",\"tasks\":[]}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(projectId, project.getLastProjectVersion(), userWorkflow.getId());

        long projectDeploymentId = deploy(projectId, userWorkflow.getId(), true, true);

        String agentWorkflowId = draftWorkflowId(agent);

        ProjectDeploymentWorkflow disabledAgentDeploymentWorkflow = new ProjectDeploymentWorkflow();

        disabledAgentDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        disabledAgentDeploymentWorkflow.setWorkflowId(agentWorkflowId);
        disabledAgentDeploymentWorkflow.setEnabled(false);

        projectDeploymentWorkflowRepository.save(disabledAgentDeploymentWorkflow);

        assertThat(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
            agentWorkflowId, Environment.DEVELOPMENT.ordinal())).isPresent();

        agentFacade.deleteAgent(agent.getId());

        assertThat(agentRepository.findById(agent.getId())).isEmpty();
        assertThat(projectService.getProject(projectId)).isNotNull();
        assertThat(projectWorkflowService.getProjectWorkflowIds(projectId)).containsExactly(userWorkflow.getId());
        assertThat(projectDeploymentWorkflowRepository.findAllByWorkflowId(agentWorkflowId)).isEmpty();
        assertThat(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
            agentWorkflowId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    @Test
    void testDeleteAgentKeepsProjectAndSiblingWorkflow() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Doomed", null, workspaceId, null);

        long projectId = agentDTO.projectId();

        Project project = projectService.getProject(projectId);

        Workflow userWorkflow = workflowService.create(
            "{\"label\":\"Survivor\",\"tasks\":[]}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(projectId, project.getLastProjectVersion(), userWorkflow.getId());

        agentFacade.deleteAgent(agentDTO.agent()
            .getId());

        assertThat(projectService.getProject(projectId)).isNotNull();
        assertThat(projectWorkflowService.getProjectWorkflowIds(projectId)).containsExactly(userWorkflow.getId());
    }

    @Test
    void testDeleteAgentKeepsSiblingAgent() {
        AiAgentDTO doomedAgentDTO = agentFacade.createAgent("Doomed", null, workspaceId, null);

        long projectId = doomedAgentDTO.projectId();

        AiAgentDTO siblingAgentDTO = agentFacade.createAgent("Sibling", null, workspaceId, projectId);

        String siblingWorkflowId = draftWorkflowId(siblingAgentDTO.agent());

        agentFacade.deleteAgent(doomedAgentDTO.agent()
            .getId());

        assertThat(agentRepository.findById(siblingAgentDTO.agent()
            .getId())).isPresent();
        assertThat(projectWorkflowService.getProjectWorkflowIds(projectId)).containsExactly(siblingWorkflowId);
        assertThat(draftDefinition(siblingAgentDTO.agent())).contains("\"name\":\"chat_1\"");
    }

    @Test
    void testPrepareProjectPublishRejectsProjectWithAgentMissingModel() {
        AiAgentDTO agentDTO = agentFacade.createAgent("No Model", null, workspaceId, null);

        assertThatThrownBy(() -> agentFacade.prepareProjectPublish(agentDTO.projectId()))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("Agent 'No Model' has no model selected.")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.MODEL_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentRejectsWhenAnySiblingAgentIsInvalid() {
        AiAgentDTO publishableAgentDTO = createPublishableAgent("Ready");

        long projectId = publishableAgentDTO.projectId();

        agentFacade.createAgent("Not Ready", null, workspaceId, projectId);

        assertThatThrownBy(() -> publishProject(publishableAgentDTO.agent(), "v1"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Agent 'Not Ready'")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.MODEL_MISSING.getErrorKey()));
    }

    @Test
    void testProjectPublishRegeneratesAgentWorkflowThroughListener() {
        AiAgentDTO agentDTO = createPublishableAgent("Listener Bot");

        long projectId = agentDTO.projectId();

        AiAgent agent = agentRepository.findById(agentDTO.agent()
            .getId())
            .orElseThrow();

        agent.setInstructions("fresh instructions");

        agentRepository.save(agent);

        String workflowId = draftWorkflowId(agent);

        assertThat(draftDefinition(agent)).doesNotContain("fresh instructions");

        projectService.publishProject(projectId, "v1", false);

        Workflow publishedWorkflow = workflowService.getWorkflow(workflowId);

        assertThat(publishedWorkflow.getDefinition()).contains("fresh instructions");
    }

    @Test
    void testProjectPublishRejectsProjectWithAgentMissingModelThroughListener() {
        AiAgentDTO agentDTO = agentFacade.createAgent("No Model", null, workspaceId, null);

        long projectId = agentDTO.projectId();

        assertThatThrownBy(() -> projectService.publishProject(projectId, "v1", false))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.MODEL_MISSING.getErrorKey()));

        Project project = projectService.getProject(projectId);

        assertThat(project.getLastPublishedProjectVersion()).isNull();
    }

    @Test
    void testDeleteProjectAgentsRemovesEveryAgentOfTheProject() {
        AiAgentDTO firstAgentDTO = agentFacade.createAgent("First", null, workspaceId, null);

        long projectId = firstAgentDTO.projectId();

        AiAgentDTO secondAgentDTO = agentFacade.createAgent("Second", null, workspaceId, projectId);
        AiAgentDTO otherProjectAgentDTO = agentFacade.createAgent("Elsewhere", null, workspaceId, null);

        agentFacade.addAgentElement(
            firstAgentDTO.agent()
                .getId(),
            AiAgentElement.KIND_SUB_AGENT, secondAgentDTO.agent()
                .getId(),
            null, null);

        agentFacade.deleteProjectAgents(projectId);

        assertThat(agentRepository.findById(firstAgentDTO.agent()
            .getId())).isEmpty();
        assertThat(agentRepository.findById(secondAgentDTO.agent()
            .getId())).isEmpty();
        assertThat(agentRepository.findById(otherProjectAgentDTO.agent()
            .getId())).isPresent();
    }

    @Test
    void testProjectDeleteListenerRemovesTheProjectAgents() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Listener Bot", null, workspaceId, null);

        long projectId = agentDTO.projectId();

        assertThat(projectDeleteEventListeners).hasSize(1);

        for (ProjectDeleteEventListener projectDeleteEventListener : projectDeleteEventListeners) {
            projectDeleteEventListener.onBeforeDeleteProject(projectId);
        }

        assertThat(agentRepository.findById(agentDTO.agent()
            .getId())).isEmpty();
    }

    @Test
    void testDeleteProjectAgentsRefusesWhileReferencedFromAnotherProject() {
        AiAgentDTO subAgentDTO = createPublishableAgent("Sub");
        AiAgentDTO parentAgentDTO = agentFacade.createAgent("Parent", null, workspaceId, null);

        publishProject(subAgentDTO.agent(), "v1");

        agentFacade.addAgentElement(
            parentAgentDTO.agent()
                .getId(),
            AiAgentElement.KIND_SUB_AGENT, subAgentDTO.agent()
                .getId(),
            Map.of(), null);

        assertThatThrownBy(() -> agentFacade.deleteProjectAgents(subAgentDTO.projectId()))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.AGENT_REFERENCED_AS_SUB_AGENT.getErrorKey()));
    }

    @Test
    void testAddSubAgentCycleThrows() {
        AiAgentDTO agentADTO = agentFacade.createAgent("Agent A", null, workspaceId, null);
        AiAgentDTO agentBDTO = agentFacade.createAgent("Agent B", null, workspaceId, null);

        AiAgent agentA = agentADTO.agent();
        AiAgent agentB = agentBDTO.agent();

        agentFacade.addAgentElement(agentA.getId(), AiAgentElement.KIND_SUB_AGENT, agentB.getId(), null, null);

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agentB.getId(), AiAgentElement.KIND_SUB_AGENT, agentA.getId(), null,
                null))
                    .isInstanceOf(ConfigurationException.class)
                    .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                        .isEqualTo(AiAgentErrorType.SUB_AGENT_CYCLE.getErrorKey()));
    }

    @Test
    void testAddSubAgentSelfReferenceThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Agent A", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_SUB_AGENT, agent.getId(), null, null))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AiAgentErrorType.SUB_AGENT_CYCLE.getErrorKey()));
    }

    @Test
    void testCreateAgentSlugUniquenessSuffixing() {
        AiAgentDTO first = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgentDTO second = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgentDTO third = agentFacade.createAgent("Support Bot", null, workspaceId, null);

        assertThat(first.agent()
            .getName()).isEqualTo("support-bot");
        assertThat(second.agent()
            .getName()).isEqualTo("support-bot-2");
        assertThat(third.agent()
            .getName()).isEqualTo("support-bot-3");
    }

    @Test
    void testAddUnknownChannelTypeThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> agentFacade.addAgentChannel(agent.getId(), "not-a-channel", null, null))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.UNKNOWN_CHANNEL_TYPE.getErrorKey()));
    }

    @Test
    void testAddDuplicateChatChannelThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> agentFacade.addAgentChannel(agent.getId(), "chat", null, null))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_ALREADY_PRESENT.getErrorKey()));
    }

    @Test
    void testAddSecondApprovalGateElementThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_GATE, null, null, null);

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_GATE, null, null, null))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AiAgentErrorType.ELEMENT_KIND_ALREADY_PRESENT.getErrorKey()));
    }

    @Test
    void testPublishAgentWithLeftoverRequiresApprovalFlagAndNoGatePublishesUngated() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_TOOL, null,
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendMessage", "requiresApproval",
                true),
            null);

        assertThatCode(() -> publishProject(agent, "First release")).doesNotThrowAnyException();
    }

    @Test
    void testExportAgentCarriesConfigurationButNoConnections() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Answers questions", workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.updateAgent(agent.getId(), "Support Bot", "Answers questions", "Be concise.");
        agentFacade.addAgentChannel(agent.getId(), SLACK_CHANNEL_TYPE, Map.of(), 77L);
        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        String exported = agentFacade.exportAgent(agent.getId());

        assertThat(exported).contains("Support Bot")
            .contains("Be concise.")
            .contains(SLACK_CHANNEL_TYPE)
            .contains("gpt-4")
            .doesNotContain("77")
            .doesNotContain("42");
    }

    @Test
    void testImportAgentRecreatesChannelsAndElements() {
        AiAgentDTO sourceDTO = agentFacade.createAgent("Support Bot", "Answers questions", workspaceId, null);
        AiAgent source = sourceDTO.agent();

        agentFacade.updateAgent(source.getId(), "Support Bot", "Answers questions", "Be concise.");
        agentFacade.addAgentChannel(source.getId(), SLACK_CHANNEL_TYPE, Map.of(), 77L);
        agentFacade.addAgentElement(
            source.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        AiAgentDTO importedDTO = agentFacade.importAgent(workspaceId, agentFacade.exportAgent(source.getId()), null);

        AiAgent imported = importedDTO.agent();

        assertThat(imported.getId()).isNotEqualTo(source.getId());
        assertThat(imported.getInstructions()).isEqualTo("Be concise.");

        assertThat(importedDTO.channels()).extracting(AiAgentChannel::getChannelType)
            .containsExactlyInAnyOrder(
                AiAgentChannelType.CHAT, AiAgentChannelType.WORKFLOW_CALL, SLACK_CHANNEL_TYPE);

        assertThat(importedDTO.elements()).extracting(AiAgentElement::getKind)
            .containsExactlyInAnyOrder(AiAgentElement.KIND_CHAT_MEMORY, AiAgentElement.KIND_MODEL);

        assertThat(importedDTO.elements()).allSatisfy(element -> assertThat(element.getConnectionId()).isNull());
    }

    @Test
    void testImportAgentSkipsReferenceCarryingElements() {
        AiAgentDTO sourceDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent source = sourceDTO.agent();

        agentFacade.addAgentElement(source.getId(), AiAgentElement.KIND_SKILL, 12345L, Map.of(), null);

        AiAgentDTO importedDTO = agentFacade.importAgent(workspaceId, agentFacade.exportAgent(source.getId()), null);

        assertThat(importedDTO.elements()).extracting(AiAgentElement::getKind)
            .doesNotContain(AiAgentElement.KIND_SKILL);
    }

    @Test
    void testImportAgentWithInvalidJsonThrows() {
        assertThatThrownBy(() -> agentFacade.importAgent(workspaceId, "not json", null))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.INVALID_AGENT_IMPORT.getErrorKey()));
    }

    @Test
    void testGetAgentVersionsReturnsNewestFirst() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        publishProject(agent, "First release");

        assertThat(agentFacade.getAgentVersions(agent.getId())).extracting(AiAgentVersionDTO::version)
            .containsExactly(2, 1);

        assertThat(agentFacade.getAgentVersions(agent.getId()))
            .first()
            .satisfies(agentVersion -> assertThat(agentVersion.status()).isEqualTo("DRAFT"));
    }

    @Test
    void testPublishAgentWithGatedToolPublishesApprovalGateDeliveringOverChat() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_GATE, null, null, null);

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_TOOL, null,
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendMessage", "requiresApproval",
                true),
            null);

        int newVersion = publishProject(agent, "First release");

        assertThat(newVersion).isEqualTo(2);

        Project project = projectService.getProject(agentService.getProjectId(agent));
        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agentService.getProjectId(agent), publishedVersion.getVersion());

        Workflow publishedWorkflow = workflowService.getWorkflow(publishedWorkflowIds.get(0));

        assertThat(publishedWorkflow.getDefinition()).contains("\"aiAgentUtils/v1/approvalGateTool\"");
        assertThat(publishedWorkflow.getDefinition()).contains("\"chat/v1/chat\"");
    }

    @Test
    void testAddSecondApprovalToolElementThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, null, null);

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, null, null))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AiAgentErrorType.ELEMENT_KIND_ALREADY_PRESENT.getErrorKey()));
    }

    @Test
    void testAddApprovalToolElementRegeneratesDefinitionWithRequestApprovalTool() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, null, null);

        assertThat(draftDefinition(agent)).contains("\"approval/v1/requestApproval\"");
    }

    @Test
    void testPublishAgentWithApprovalToolPublishesRequestApprovalTool() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, null, null);

        int newVersion = publishProject(agent, "First release");

        assertThat(newVersion).isEqualTo(2);

        Project project = projectService.getProject(agentService.getProjectId(agent));
        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agentService.getProjectId(agent), publishedVersion.getVersion());

        Workflow publishedWorkflow = workflowService.getWorkflow(publishedWorkflowIds.get(0));

        assertThat(publishedWorkflow.getDefinition()).contains("\"approval/v1/requestApproval\"");
    }

    @Test
    void testUpdateAgentSettingsRegeneratesDefinitionWithWebSearchTool() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThat(draftDefinition(agent)).doesNotContain("brave/v1/webSearch");

        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 42)));

        assertThat(draftDefinition(agent)).contains("\"brave/v1/webSearch\"");
    }

    @Test
    void testUpdateAgentSettingsWithConnectionIdSyncsWorkflowTestConfigurationConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 42)));

        String workflowId = draftWorkflowId(agent);

        WorkflowTestConfiguration workflowTestConfiguration = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, Environment.DEVELOPMENT.ordinal())
            .orElseThrow();

        assertThat(workflowTestConfiguration.getConnections())
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(org.assertj.core.groups.Tuple.tuple("aiAgent_1", "brave_1", 42L));
    }

    @Test
    void testPublishAgentWithWebSearchEnabledAndNoConnectionThrowsBuiltInToolConnectionMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(agent.getId(), Map.of("builtInTools", Map.of("webSearch", true)));

        assertThatThrownBy(() -> publishProject(agent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("has web search enabled but no web search connection selected.")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.BUILT_IN_TOOL_CONNECTION_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentWithWebSearchEnabledAndConnectionPublishes() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 42)));

        assertThatCode(() -> publishProject(agent, "First release")).doesNotThrowAnyException();
    }

    @Test
    void testUpdateAgentSettingsWithFirecrawlProviderRegeneratesDefinitionWithFirecrawlSearchTool() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.updateAgentSettings(
            agent.getId(),
            Map.of(
                "builtInTools",
                Map.of("webSearch", true, "webSearchProvider", "FIRECRAWL", "webSearchConnectionId", 42)));

        String definition = draftDefinition(agent);

        assertThat(definition).contains("\"firecrawl/v1/search\"");
        assertThat(definition).doesNotContain("brave/v1/webSearch");
    }

    @Test
    void testPublishAgentWithNativeWebSearchAndNoConnectionPublishesOnASupportedModelProvider() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null,
            Map.of("provider", "anthropic", "model", "claude-sonnet-4-5"), null);
        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchProvider", "NATIVE")));

        assertThatCode(() -> publishProject(agent, "First release")).doesNotThrowAnyException();
    }

    @Test
    void testPublishAgentWithNativeWebSearchOnAnUnsupportedModelProviderThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchProvider", "NATIVE")));

        assertThatThrownBy(() -> publishProject(agent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.NATIVE_WEB_SEARCH_UNSUPPORTED.getErrorKey()));
    }

    @Test
    void testPublishAgentWithThinkingPublishesOnASupportedModelProvider() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null,
            Map.of("provider", "anthropic", "model", "claude-sonnet-4-5"), null);
        agentFacade.updateAgentSettings(agent.getId(), Map.of("thinking", true, "reasoningEffort", "HIGH"));

        assertThatCode(() -> publishProject(agent, "First release")).doesNotThrowAnyException();
    }

    @Test
    void testPublishAgentWithThinkingOnAnUnsupportedModelProviderThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openRouter", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(agent.getId(), Map.of("thinking", true));

        assertThatThrownBy(() -> publishProject(agent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.THINKING_UNSUPPORTED.getErrorKey()));
    }

    @Test
    void testPublishAgentWithThinkingOffIsNotProviderChecked() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openRouter", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(agent.getId(), Map.of("thinking", false));

        assertThatCode(() -> publishProject(agent, "First release")).doesNotThrowAnyException();
    }

    @Test
    void testUpdateAgentSettingsWithMaxToolCallsRegeneratesDefinitionWithIt() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.updateAgentSettings(agent.getId(), Map.of("maxToolCalls", 30));

        assertThat(draftDefinition(agent)).contains("\"maxToolCalls\":30");
    }

    @Test
    void testDeleteScheduleChannelRegeneratesDefinitionWithoutIt() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentChannel scheduleChannel = agentFacade.addAgentChannel(
            agent.getId(), "schedule", Map.of("expression", "0 0 * * * *", "prompt", "Summarize"), null);

        assertThat(draftDefinition(agent)).contains("\"name\":\"schedule_1\"");

        agentFacade.deleteAgentChannel(scheduleChannel.getId());

        assertThat(draftDefinition(agent)).doesNotContain("\"name\":\"schedule_1\"");
    }

    @Test
    void testDeleteAgentRemovesAgentAndWorkflowRowsButKeepsProject() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();
        long projectId = agentService.getProjectId(agent);

        List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(projectId, 1);

        assertThat(workflowIds).hasSize(1);

        agentFacade.deleteAgent(agent.getId());

        assertThat(agentRepository.findById(agent.getId())).isEmpty();
        assertThat(projectRepository.findById(projectId)).isPresent();
        assertThat(projectWorkflowService.getProjectWorkflowIds(projectId, 1)).isEmpty();

        for (String workflowId : workflowIds) {
            assertThat(workflowCrudRepository.findById(workflowId)).isEmpty();
        }
    }

    @Test
    void testUpdateAgentUpdatesTitleDescriptionAndInstructions() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Old description", workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentDTO updated = agentFacade.updateAgent(agent.getId(), "Updated Title", "Updated description", "Be nice");

        assertThat(updated.agent()
            .getTitle()).isEqualTo("Updated Title");
        assertThat(updated.agent()
            .getDescription()).isEqualTo("Updated description");
        assertThat(updated.agent()
            .getInstructions()).isEqualTo("Be nice");
    }

    @Test
    void testUpdateAgentNullDescriptionLeavesExistingDescriptionUnchanged() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Original description", workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentDTO updated = agentFacade.updateAgent(agent.getId(), null, null, "Be nice");

        assertThat(updated.agent()
            .getDescription()).isEqualTo("Original description");
        assertThat(updated.agent()
            .getInstructions()).isEqualTo("Be nice");
    }

    @Test
    void testUpdateAgentNullInstructionsLeavesExistingInstructionsUnchanged() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.updateAgent(agent.getId(), null, null, "Be nice");

        AiAgentDTO updated = agentFacade.updateAgent(agent.getId(), null, "New description", null);

        assertThat(updated.agent()
            .getDescription()).isEqualTo("New description");
        assertThat(updated.agent()
            .getInstructions()).isEqualTo("Be nice");
    }

    @Test
    void testDeleteAgentElementRemovesElement() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentElement modelElement = agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        agentFacade.deleteAgentElement(modelElement.getId());

        assertThat(agentElementRepository.findById(modelElement.getId())).isEmpty();
        assertThat(draftDefinition(agent)).doesNotContain("openai/v1/model");
    }

    @Test
    void testGetAgentAndGetAgentsReturnPersistedAgent() {
        AiAgentDTO created = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = created.agent();

        AiAgentDTO fetched = agentFacade.getAgent(agent.getId());

        assertThat(fetched.agent()
            .getId()).isEqualTo(agent.getId());

        assertThat(agentFacade.getAgents(workspaceId))
            .extracting(dto -> dto.agent()
                .getId())
            .containsExactly(agent.getId());
    }

    @Test
    void testUpdateAgentChannelNullConnectionIdLeavesExistingConnectionIdUnchanged() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentChannel scheduleChannel = agentFacade.addAgentChannel(
            agent.getId(), "schedule", Map.of("expression", "0 0 * * * *", "prompt", "Summarize"), 42L);

        agentFacade.updateAgentChannel(
            scheduleChannel.getId(), Map.of("expression", "0 30 * * * *", "prompt", "Summarize"), null);

        AiAgentChannel reloaded = agentChannelRepository.findById(scheduleChannel.getId())
            .orElseThrow();

        assertThat(reloaded.getConnectionId()).isEqualTo(42L);
        assertThat(reloaded.getParameters()
            .get("expression")).isEqualTo("0 30 * * * *");
    }

    @Test
    void testUpdateAgentChannelNonNullConnectionIdReplacesExistingValue() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentChannel scheduleChannel = agentFacade.addAgentChannel(
            agent.getId(), "schedule", Map.of("expression", "0 0 * * * *", "prompt", "Summarize"), 42L);

        agentFacade.updateAgentChannel(scheduleChannel.getId(), null, 99L);

        AiAgentChannel reloaded = agentChannelRepository.findById(scheduleChannel.getId())
            .orElseThrow();

        assertThat(reloaded.getConnectionId()).isEqualTo(99L);
        assertThat(reloaded.getParameters()
            .get("expression")).isEqualTo("0 0 * * * *");
    }

    @Test
    void testUpdateAgentElementNullConnectionIdLeavesExistingConnectionIdUnchanged() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentElement modelElement = agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        agentFacade.updateAgentElement(
            modelElement.getId(), Map.of("provider", "openai", "model", "gpt-4o"), null);

        AiAgentElement reloaded = agentElementRepository.findById(modelElement.getId())
            .orElseThrow();

        assertThat(reloaded.getConnectionId()).isEqualTo(42L);
        assertThat(reloaded.getParameters()
            .get("model")).isEqualTo("gpt-4o");
    }

    @Test
    void testUpdateAgentElementNonNullConnectionIdReplacesExistingValue() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        AiAgentElement modelElement = agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        agentFacade.updateAgentElement(modelElement.getId(), null, 99L);

        AiAgentElement reloaded = agentElementRepository.findById(modelElement.getId())
            .orElseThrow();

        assertThat(reloaded.getConnectionId()).isEqualTo(99L);
        assertThat(reloaded.getParameters()
            .get("model")).isEqualTo("gpt-4");
    }

    @Test
    void testPublishAgentWithoutModelThrowsModelMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> publishProject(agent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.MODEL_MISSING.getErrorKey()));
    }

    @Test
    void testProjectWithAnUnconnectedChannelIsNotPublishable() {
        AiAgent agent = agentFacade.createAgent("Support Bot", null, workspaceId, null)
            .agent();
        long projectId = agentService.getProjectId(agent);

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        assertThat(agentFacade.isProjectPublishable(projectId)).isTrue();

        AiAgentChannel telegramChannel = agentFacade.addAgentChannel(agent.getId(), "telegram", null, null);

        assertThat(agentFacade.isProjectPublishable(projectId)).isFalse();

        agentFacade.updateAgentChannel(telegramChannel.getId(), Map.of(), 77L);

        assertThat(agentFacade.isProjectPublishable(projectId)).isTrue();
    }

    @Test
    void testPublishAgentWithUnconnectedTelegramChannelThrowsChannelConnectionMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentChannel(agent.getId(), "telegram", null, null);

        assertThatThrownBy(() -> publishProject(agent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("Agent 'Support Bot' has a 'telegram' channel with no connection selected.")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_CONNECTION_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentWithMissingRequiredChannelParameterThrowsChannelParameterMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentChannel(agent.getId(), "twilio", null, null);

        assertThatThrownBy(() -> publishProject(agent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageStartingWith("Agent 'Support Bot' has a 'twilio' channel without parameter")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_PARAMETER_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentWithSuppliedRequiredChannelParameterPublishes() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentChannel(agent.getId(), "twilio", Map.of("number", "+15550000000"), null);

        assertThat(publishProject(agent, "First release")).isPositive();
    }

    @Test
    void testPublishAgentWithUnpublishedSubAgentThrowsSubAgentNotPublished() {
        AiAgentDTO parentAgentDTO = agentFacade.createAgent("Parent Bot", null, workspaceId, null);
        AiAgentDTO subAgentDTO = agentFacade.createAgent("Sub Bot", null, workspaceId, null);

        AiAgent parentAgent = parentAgentDTO.agent();
        AiAgent subAgent = subAgentDTO.agent();

        agentFacade.addAgentElement(
            parentAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(parentAgent.getId(), AiAgentElement.KIND_SUB_AGENT, subAgent.getId(), null, null);

        assertThatThrownBy(() -> publishProject(parentAgent, "First release"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("Agent 'Parent Bot' uses sub-agent 'Sub Bot', whose project has no published version.")
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.SUB_AGENT_NOT_PUBLISHED.getErrorKey()));
    }

    @Test
    void testPublishAgentWithSubAgentOfTheSameUnpublishedProjectSucceeds() {
        AiAgentDTO parentAgentDTO = agentFacade.createAgent("Parent Bot", null, workspaceId, null);
        AiAgent parentAgent = parentAgentDTO.agent();

        AiAgent subAgent = agentFacade.createAgent(
            "Sub Bot", null, workspaceId, agentService.getProjectId(parentAgent))
            .agent();

        agentFacade.addAgentElement(
            parentAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(
            subAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(parentAgent.getId(), AiAgentElement.KIND_SUB_AGENT, subAgent.getId(), null, null);

        assertThat(publishProject(parentAgent, "First release")).isPositive();
    }

    @Test
    void testPublishAgentHappyPathReturnsVersionAndPublishesWorkflowWithTriggers() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        int newVersion = publishProject(agent, "First release");

        assertThat(newVersion).isEqualTo(2);

        Project project = projectService.getProject(agentService.getProjectId(agent));
        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        assertThat(publishedVersion).isNotNull();
        assertThat(publishedVersion.getVersion()).isEqualTo(1);

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agentService.getProjectId(agent), publishedVersion.getVersion());

        assertThat(publishedWorkflowIds).hasSize(1);

        Workflow publishedWorkflow = workflowService.getWorkflow(publishedWorkflowIds.get(0));

        assertThat(publishedWorkflow.getDefinition()).contains("\"name\":\"chat_1\"");
        assertThat(publishedWorkflow.getDefinition()).contains("\"name\":\"workflowCall_1\"");
    }

    @Test
    void testPublishAgentFlipsUnpublishedChangesThenSubsequentMutationFlipsItBack() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        assertThat(agentFacade.getAgent(agent.getId())
            .unpublishedChanges()).isTrue();

        publishProject(agent, "First release");

        assertThat(agentFacade.getAgent(agent.getId())
            .unpublishedChanges()).isFalse();

        agentFacade.updateAgent(agent.getId(), "Support Bot", null, "Be nice");

        assertThat(agentFacade.getAgent(agent.getId())
            .unpublishedChanges()).isTrue();
    }

    @Test
    void testDeleteAgentAfterPublishRemovesWorkflowsAcrossAllVersions() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();
        long projectId = agentService.getProjectId(agent);

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        publishProject(agent, "First release");

        Project project = projectService.getProject(projectId);
        List<String> allWorkflowIds = new ArrayList<>();

        for (ProjectVersion projectVersion : project.getProjectVersions()) {
            allWorkflowIds.addAll(
                projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion.getVersion()));
        }

        assertThat(allWorkflowIds).hasSize(2);

        agentFacade.deleteAgent(agent.getId());

        assertThat(projectRepository.findById(projectId)).isPresent();
        assertThat(projectWorkflowService.getProjectWorkflowIds(projectId)).isEmpty();

        for (String workflowId : allWorkflowIds) {
            assertThat(workflowCrudRepository.findById(workflowId)).isEmpty();
        }
    }

    @Test
    void testGetAgentChannelDefinitionsIncludesSynthesizedScheduleEntry() {
        List<ResolvedAgentChannel> channelDefinitions = agentFacade.getAgentChannelDefinitions();

        assertThat(channelDefinitions)
            .extracting(ResolvedAgentChannel::name)
            .contains(
                AiAgentChannelType.CHAT, AiAgentChannelType.WORKFLOW_CALL, AiAgentChannelType.SCHEDULE, "slack",
                "telegram", "twilio");

        assertThat(channelDefinitions)
            .allSatisfy(channelDefinition -> assertThat(channelDefinition.title()).isNotBlank());

        ResolvedAgentChannel scheduleChannelDefinition = channelDefinitions.stream()
            .filter(channelDefinition -> AiAgentChannelType.SCHEDULE.equals(channelDefinition.name()))
            .findFirst()
            .orElseThrow();

        assertThat(scheduleChannelDefinition.title()).isEqualTo("Schedule");
        assertThat(scheduleChannelDefinition.triggerType()).isEqualTo("schedule/v1/cron");
        assertThat(scheduleChannelDefinition.replyActionType()).isNull();
        assertThat(scheduleChannelDefinition.connectionRequired()).isFalse();
        assertThat(scheduleChannelDefinition.approvalDelivery()).isNull();
    }

    @Test
    void testGetWorkspaceChatAgentsReturnsDeployedChatAgent() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        String workflowId = draftWorkflowId(agent);

        long projectDeploymentId = deploy(agentService.getProjectId(agent), workflowId, true, true);

        stubStaticWebhookChatTriggerDefinition();

        List<ChatAgentDTO> chatAgents = agentFacade.getWorkspaceChatAgents(
            workspaceId, Environment.DEVELOPMENT.ordinal());

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        WorkflowExecutionId expectedWorkflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, projectDeploymentId, projectWorkflow.getUuidAsString(), CHAT_TRIGGER_NAME);

        assertThat(chatAgents).containsExactly(
            new ChatAgentDTO(
                agent.getId(), "support-bot", "Support Bot", projectDeploymentId,
                expectedWorkflowExecutionId.toString(), workflowService.getWorkflow(workflowId)
                    .getLabel()));
    }

    @Test
    void testGetWorkspaceChatAgentsSkipsDisabledDeployment() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        deploy(agentService.getProjectId(agent), draftWorkflowId(agent), false, true);

        stubStaticWebhookChatTriggerDefinition();

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    @Test
    void testGetWorkspaceChatAgentsSkipsDisabledDeploymentWorkflow() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        deploy(agentService.getProjectId(agent), draftWorkflowId(agent), true, false);

        stubStaticWebhookChatTriggerDefinition();

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    @Test
    void testGetWorkspaceChatAgentsSkipsWorkflowWithoutChatTrigger() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        String workflowId = draftWorkflowId(agent);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        workflowService.update(
            workflowId, """
                {
                    "label": "Scheduled Bot",
                    "triggers": [
                        {"name": "trigger_1", "label": "Cron", "type": "schedule/v1/cron", "parameters": {}}
                    ],
                    "tasks": []
                }
                """, workflow.getVersion());

        deploy(agentService.getProjectId(agent), workflowId, true, true);

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    @Test
    void testGetWorkspaceChatAgentsSkipsOtherEnvironment() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId, null);
        AiAgent agent = agentDTO.agent();

        deploy(agentService.getProjectId(agent), draftWorkflowId(agent), true, true);

        stubStaticWebhookChatTriggerDefinition();

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.PRODUCTION.ordinal())).isEmpty();
    }

    @Test
    void testAgentDeploymentListsOnlyTheAgentsOwnWorkflow() {
        AiAgentDTO agentDTO = createPublishableAgent("Deployed Bot");

        long projectId = agentDTO.projectId();

        Project project = projectService.getProject(projectId);

        Workflow userWorkflow = workflowService.create(
            "{\"label\":\"Sibling\",\"tasks\":[]}", Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectWorkflowService.addWorkflow(projectId, project.getLastProjectVersion(), userWorkflow.getId());

        int nextDraftVersion = publishProject(agentDTO.agent(), "v1");

        deployProjectVersion(projectId, nextDraftVersion - 1);

        stubNonWebhookTriggerDefinitions();

        assertThat(agentFacade.getAgentDeployments(workspaceId))
            .singleElement()
            .satisfies(agentDeploymentDTO -> assertThat(agentDeploymentDTO.workflows()).hasSize(1));
    }

    private void stubNonWebhookTriggerDefinitions() {
        TriggerDefinition triggerDefinition = Mockito.mock(TriggerDefinition.class);

        Mockito.when(triggerDefinition.getType())
            .thenReturn(TriggerType.DYNAMIC_WEBHOOK);

        Mockito.when(
            triggerDefinitionService.getTriggerDefinition(
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyString()))
            .thenReturn(triggerDefinition);
    }

    private long deployProjectVersion(long projectId, int projectVersion) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setEnabled(true);

        ProjectDeployment savedProjectDeployment = projectDeploymentRepository.save(projectDeployment);

        for (String workflowId : projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion)) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

            projectDeploymentWorkflow.setProjectDeploymentId(savedProjectDeployment.getId());
            projectDeploymentWorkflow.setWorkflowId(workflowId);
            projectDeploymentWorkflow.setEnabled(true);

            projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);
        }

        return savedProjectDeployment.getId();
    }

    private long deploy(
        long projectId, String workflowId, boolean deploymentEnabled, boolean deploymentWorkflowEnabled) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setEnabled(deploymentEnabled);

        ProjectDeployment savedProjectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(savedProjectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);
        projectDeploymentWorkflow.setEnabled(deploymentWorkflowEnabled);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        return savedProjectDeployment.getId();
    }

    private void stubStaticWebhookChatTriggerDefinition() {
        TriggerDefinition triggerDefinition = Mockito.mock(TriggerDefinition.class);

        Mockito.when(triggerDefinition.getType())
            .thenReturn(TriggerType.STATIC_WEBHOOK);
        Mockito.when(triggerDefinition.getName())
            .thenReturn("newChatRequest");

        Mockito.when(triggerDefinitionService.getTriggerDefinition("chat", 1, "newChatRequest"))
            .thenReturn(triggerDefinition);
    }

    private AiAgentDTO createPublishableAgent(String title) {
        AiAgentDTO agentDTO = agentFacade.createAgent(title, null, workspaceId, null);

        agentFacade.addAgentElement(
            agentDTO.agent()
                .getId(),
            AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        return agentDTO;
    }

    private String draftDefinition(AiAgent agent) {
        Workflow workflow = workflowService.getWorkflow(draftWorkflowId(agent));

        return workflow.getDefinition();
    }

    private List<WorkflowTestConfigurationConnection> draftTestConnections(AiAgent agent) {
        WorkflowTestConfiguration workflowTestConfiguration = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(draftWorkflowId(agent), Environment.DEVELOPMENT.ordinal())
            .orElseThrow();

        return workflowTestConfiguration.getConnections();
    }

    private String draftWorkflowId(AiAgent agent) {
        return agentFacade.getDraftWorkflowId(agent.getId());
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

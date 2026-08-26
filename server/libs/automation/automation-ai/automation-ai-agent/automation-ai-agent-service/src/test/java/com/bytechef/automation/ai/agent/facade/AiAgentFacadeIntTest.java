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
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Integration test for {@link AiAgentFacadeImpl}: hidden-project provisioning, draft-workflow regeneration on every
 * save, and the delete/duplicate/cycle guard rails.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationAiAgentIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
class AiAgentFacadeIntTest {

    /**
     * The workflow node name {@code AiAgentWorkflowGenerator} gives the permanent {@code chat} channel's trigger —
     * {@code <channelType>_<nth occurrence>}, and the chat channel is always emitted first.
     */
    private static final String CHAT_TRIGGER_NAME = "chat_1";

    /**
     * A discovered channel key: only chat/workflowCall/schedule have an {@link AiAgentChannelType} constant, every
     * other channel is whatever a component declares. See {@code TestComponentDefinitions} for the stubbed slack
     * component this slice resolves it against.
     */
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

    private Long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("test-workspace"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        // triggerDefinitionService is a context-scoped Mockito mock (see AutomationAiAgentIntTestConfiguration), so
        // per-test stubbing would otherwise leak into every later test sharing the same Spring context.
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
    void testCreateAgentProvisionsHiddenProjectAndDraftWorkflow() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Handles support questions", workspaceId);

        AiAgent agent = agentDTO.agent();
        Project project = projectService.getProject(agent.getProjectId());

        assertThat(project.getName()).startsWith(SystemProjects.AI_AGENT_NAME_PREFIX);

        String definition = draftDefinition(agent.getProjectId());

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
    void testAddModelElementRegeneratesDefinitionWithModelClusterElement() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        String definition = draftDefinition(agent.getProjectId());

        assertThat(definition).contains("\"openai/v1/model\"");
    }

    @Test
    void testAddModelElementWithConnectionIdSyncsWorkflowTestConfigurationConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        String workflowId = draftWorkflowId(agent.getProjectId());

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

    /**
     * An approval delivered over Slack reuses the connection of the agent's own Slack channel, so Slack is configured
     * once rather than twice — the delivery node is derived from the {@code AiAgentChannel} row and carries that row's
     * connection.
     * <p>
     * The slack TOOL element ahead of it only pushes the delivery node's name to {@code slack_2}, keeping the two rows
     * visibly distinct in the assertion.
     */
    @Test
    void testApprovalDeliveryReusesTheAgentChannelConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentChannel(agent.getId(), SLACK_CHANNEL_TYPE, Map.of(), 77L);

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_TOOL, null,
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendChannelMessage", "parameters",
                Map.of()),
            null);

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, Map.of(), null);

        assertThat(draftTestConnections(agent.getProjectId()))
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(org.assertj.core.groups.Tuple.tuple("aiAgent_1", "slack_2", 77L));
    }

    @Test
    void testAddSecondModelElementThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);

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
        AiAgentDTO parentAgentDTO = agentFacade.createAgent("Parent Bot", null, workspaceId);
        AiAgentDTO subAgentDTO = agentFacade.createAgent("Sub Bot", null, workspaceId);

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
    void testDeleteAgentWithDeploymentsThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(agent.getProjectId());
        projectDeployment.setProjectVersion(1);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setEnabled(true);
        projectDeployment.setUuid(UUID.randomUUID());

        projectDeploymentRepository.save(projectDeployment);

        assertThatThrownBy(() -> agentFacade.deleteAgent(agent.getId()))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.AGENT_HAS_DEPLOYMENTS.getErrorKey()));
    }

    @Test
    void testAddSubAgentCycleThrows() {
        AiAgentDTO agentADTO = agentFacade.createAgent("Agent A", null, workspaceId);
        AiAgentDTO agentBDTO = agentFacade.createAgent("Agent B", null, workspaceId);

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
        AiAgentDTO agentDTO = agentFacade.createAgent("Agent A", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_SUB_AGENT, agent.getId(), null, null))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AiAgentErrorType.SUB_AGENT_CYCLE.getErrorKey()));
    }

    @Test
    void testCreateAgentSlugUniquenessSuffixing() {
        AiAgentDTO first = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgentDTO second = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgentDTO third = agentFacade.createAgent("Support Bot", null, workspaceId);

        assertThat(first.agent()
            .getName()).isEqualTo("support-bot");
        assertThat(second.agent()
            .getName()).isEqualTo("support-bot-2");
        assertThat(third.agent()
            .getName()).isEqualTo("support-bot-3");
    }

    @Test
    void testAddUnknownChannelTypeThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> agentFacade.addAgentChannel(agent.getId(), "not-a-channel", null, null))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.UNKNOWN_CHANNEL_TYPE.getErrorKey()));
    }

    @Test
    void testAddDuplicateChatChannelThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> agentFacade.addAgentChannel(agent.getId(), "chat", null, null))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_ALREADY_PRESENT.getErrorKey()));
    }

    @Test
    void testAddSecondApprovalGateElementThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_GATE, null, null, null);

        assertThatThrownBy(
            () -> agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_GATE, null, null, null))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                    .isEqualTo(AiAgentErrorType.ELEMENT_KIND_ALREADY_PRESENT.getErrorKey()));
    }

    /**
     * A tool's {@code requiresApproval} flag survives the APPROVAL_GATE master switch being turned off, so that turning
     * it back on restores the previous gating. With the switch off nothing gates, so the published workflow carries no
     * gate at all.
     */
    @Test
    void testPublishAgentWithLeftoverRequiresApprovalFlagAndNoGatePublishesUngated() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_TOOL, null,
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendMessage", "requiresApproval",
                true),
            null);

        assertThatCode(() -> agentFacade.publishAgent(agent.getId(), "First release")).doesNotThrowAnyException();
    }

    @Test
    void testExportAgentCarriesConfigurationButNoConnections() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Answers questions", workspaceId);
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
            // Connection ids belong to a workspace and an environment; carrying one across would dangle or point
            // at someone else's credential.
            .doesNotContain("77")
            .doesNotContain("42");
    }

    @Test
    void testImportAgentRecreatesChannelsAndElements() {
        AiAgentDTO sourceDTO = agentFacade.createAgent("Support Bot", "Answers questions", workspaceId);
        AiAgent source = sourceDTO.agent();

        agentFacade.updateAgent(source.getId(), "Support Bot", "Answers questions", "Be concise.");
        agentFacade.addAgentChannel(source.getId(), SLACK_CHANNEL_TYPE, Map.of(), 77L);
        agentFacade.addAgentElement(
            source.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), 42L);

        AiAgentDTO importedDTO = agentFacade.importAgent(workspaceId, agentFacade.exportAgent(source.getId()));

        AiAgent imported = importedDTO.agent();

        assertThat(imported.getId()).isNotEqualTo(source.getId());
        assertThat(imported.getInstructions()).isEqualTo("Be concise.");

        assertThat(importedDTO.channels()).extracting(AiAgentChannel::getChannelType)
            .containsExactlyInAnyOrder(
                AiAgentChannelType.CHAT, AiAgentChannelType.WORKFLOW_CALL, SLACK_CHANNEL_TYPE);

        // Exactly one chat memory: createAgent adds it, and the imported one must not add a second.
        assertThat(importedDTO.elements()).extracting(AiAgentElement::getKind)
            .containsExactlyInAnyOrder(AiAgentElement.KIND_CHAT_MEMORY, AiAgentElement.KIND_MODEL);

        assertThat(importedDTO.elements()).allSatisfy(element -> assertThat(element.getConnectionId()).isNull());
    }

    @Test
    void testImportAgentSkipsReferenceCarryingElements() {
        AiAgentDTO sourceDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent source = sourceDTO.agent();

        agentFacade.addAgentElement(source.getId(), AiAgentElement.KIND_SKILL, 12345L, Map.of(), null);

        AiAgentDTO importedDTO = agentFacade.importAgent(workspaceId, agentFacade.exportAgent(source.getId()));

        // A skill id means nothing in the target workspace, so the row is dropped rather than left dangling.
        assertThat(importedDTO.elements()).extracting(AiAgentElement::getKind)
            .doesNotContain(AiAgentElement.KIND_SKILL);
    }

    @Test
    void testImportAgentWithInvalidJsonThrows() {
        assertThatThrownBy(() -> agentFacade.importAgent(workspaceId, "not json"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.INVALID_AGENT_IMPORT.getErrorKey()));
    }

    @Test
    void testGetAgentVersionsReturnsNewestFirst() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        agentFacade.publishAgent(agent.getId(), "First release");

        assertThat(agentFacade.getAgentVersions(agent.getId())).extracting(AiAgentVersionDTO::version)
            .containsExactly(2, 1);

        assertThat(agentFacade.getAgentVersions(agent.getId()))
            .first()
            .satisfies(agentVersion -> assertThat(agentVersion.status()).isEqualTo("DRAFT"));
    }

    @Test
    void testPublishAgentWithGatedToolPublishesApprovalGateDeliveringOverChat() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        // The APPROVAL_GATE row is the agent-level master switch: without it buildToolSequence emits every tool
        // ungated regardless of its own requiresApproval flag, so gating needs both the row and the flag.
        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_GATE, null, null, null);

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_TOOL, null,
            Map.of(
                "componentName", "slack", "componentVersion", 1, "actionName", "sendMessage", "requiresApproval",
                true),
            null);

        int newVersion = agentFacade.publishAgent(agent.getId(), "First release");

        assertThat(newVersion).isEqualTo(2);

        Project project = projectService.getProject(agent.getProjectId());
        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agent.getProjectId(), publishedVersion.getVersion());

        Workflow publishedWorkflow = workflowService.getWorkflow(publishedWorkflowIds.get(0));

        assertThat(publishedWorkflow.getDefinition()).contains("\"aiAgentUtils/v1/approvalGateTool\"");
        assertThat(publishedWorkflow.getDefinition()).contains("\"chat/v1/chat\"");
    }

    @Test
    void testAddSecondApprovalToolElementThrows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, null, null);

        assertThat(draftDefinition(agent.getProjectId())).contains("\"approval/v1/requestApproval\"");
    }

    @Test
    void testPublishAgentWithApprovalToolPublishesRequestApprovalTool() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(agent.getId(), AiAgentElement.KIND_APPROVAL_TOOL, null, null, null);

        int newVersion = agentFacade.publishAgent(agent.getId(), "First release");

        assertThat(newVersion).isEqualTo(2);

        Project project = projectService.getProject(agent.getProjectId());
        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agent.getProjectId(), publishedVersion.getVersion());

        Workflow publishedWorkflow = workflowService.getWorkflow(publishedWorkflowIds.get(0));

        assertThat(publishedWorkflow.getDefinition()).contains("\"approval/v1/requestApproval\"");
    }

    @Test
    void testUpdateAgentSettingsRegeneratesDefinitionWithWebSearchTool() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        assertThat(draftDefinition(agent.getProjectId())).doesNotContain("brave/v1/webSearch");

        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 42)));

        assertThat(draftDefinition(agent.getProjectId())).contains("\"brave/v1/webSearch\"");
    }

    @Test
    void testUpdateAgentSettingsWithConnectionIdSyncsWorkflowTestConfigurationConnection() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 42)));

        String workflowId = draftWorkflowId(agent.getProjectId());

        WorkflowTestConfiguration workflowTestConfiguration = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, Environment.DEVELOPMENT.ordinal())
            .orElseThrow();

        // webSearch is the brave component's own tool element, so it draws from the brave counter rather than the
        // aiAgentUtils one the other built-ins share — see AiAgentWorkflowGenerator.buildWebSearchToolElement.
        assertThat(workflowTestConfiguration.getConnections())
            .extracting(
                WorkflowTestConfigurationConnection::getWorkflowNodeName,
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId)
            .contains(org.assertj.core.groups.Tuple.tuple("aiAgent_1", "brave_1", 42L));
    }

    @Test
    void testPublishAgentWithWebSearchEnabledAndNoConnectionThrowsBuiltInToolConnectionMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(agent.getId(), Map.of("builtInTools", Map.of("webSearch", true)));

        assertThatThrownBy(() -> agentFacade.publishAgent(agent.getId(), "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.BUILT_IN_TOOL_CONNECTION_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentWithWebSearchEnabledAndConnectionPublishes() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.updateAgentSettings(
            agent.getId(), Map.of("builtInTools", Map.of("webSearch", true, "webSearchConnectionId", 42)));

        assertThatCode(() -> agentFacade.publishAgent(agent.getId(), "First release")).doesNotThrowAnyException();
    }

    @Test
    void testDeleteScheduleChannelRegeneratesDefinitionWithoutIt() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        AiAgentChannel scheduleChannel = agentFacade.addAgentChannel(
            agent.getId(), "schedule", Map.of("expression", "0 0 * * * *", "prompt", "Summarize"), null);

        assertThat(draftDefinition(agent.getProjectId())).contains("\"name\":\"schedule_1\"");

        agentFacade.deleteAgentChannel(scheduleChannel.getId());

        assertThat(draftDefinition(agent.getProjectId())).doesNotContain("\"name\":\"schedule_1\"");
    }

    @Test
    void testDeleteAgentRemovesAgentProjectAndWorkflowRows() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();
        long projectId = agent.getProjectId();

        List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(projectId, 1);

        assertThat(workflowIds).hasSize(1);

        agentFacade.deleteAgent(agent.getId());

        assertThat(agentRepository.findById(agent.getId())).isEmpty();
        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(projectWorkflowService.getProjectWorkflowIds(projectId, 1)).isEmpty();

        for (String workflowId : workflowIds) {
            assertThat(workflowCrudRepository.findById(workflowId)).isEmpty();
        }
    }

    @Test
    void testUpdateAgentUpdatesTitleDescriptionAndInstructions() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Old description", workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", "Original description", workspaceId);
        AiAgent agent = agentDTO.agent();

        // AgentInstructionsCard.tsx (the only client caller) sends only instructions on every edit — a null
        // description here must not wipe the existing value (it also feeds a SUB_AGENT's toolDescription).
        AiAgentDTO updated = agentFacade.updateAgent(agent.getId(), null, null, "Be nice");

        assertThat(updated.agent()
            .getDescription()).isEqualTo("Original description");
        assertThat(updated.agent()
            .getInstructions()).isEqualTo("Be nice");
    }

    @Test
    void testUpdateAgentNullInstructionsLeavesExistingInstructionsUnchanged() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        AiAgentElement modelElement = agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        agentFacade.deleteAgentElement(modelElement.getId());

        assertThat(agentElementRepository.findById(modelElement.getId())).isEmpty();
        assertThat(draftDefinition(agent.getProjectId())).doesNotContain("openai/v1/model");
    }

    @Test
    void testGetAgentAndGetAgentsReturnPersistedAgent() {
        AiAgentDTO created = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        assertThatThrownBy(() -> agentFacade.publishAgent(agent.getId(), "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.MODEL_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentWithUnconnectedTelegramChannelThrowsChannelConnectionMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentChannel(agent.getId(), "telegram", null, null);

        assertThatThrownBy(() -> agentFacade.publishAgent(agent.getId(), "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_CONNECTION_MISSING.getErrorKey()));
    }

    /**
     * The test twilio channel maps its row's {@code number} onto the reply action's REQUIRED {@code From} property, so
     * a row that does not carry {@code number} generates a reply task with no sender. Publish must refuse it rather
     * than let the agent fail on its first answer — the generator omits an unset mapped parameter by design, and
     * nothing downstream notices.
     */
    @Test
    void testPublishAgentWithMissingRequiredChannelParameterThrowsChannelParameterMissing() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentChannel(agent.getId(), "twilio", null, null);

        assertThatThrownBy(() -> agentFacade.publishAgent(agent.getId(), "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.CHANNEL_PARAMETER_MISSING.getErrorKey()));
    }

    @Test
    void testPublishAgentWithSuppliedRequiredChannelParameterPublishes() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentChannel(agent.getId(), "twilio", Map.of("number", "+15550000000"), null);

        assertThat(agentFacade.publishAgent(agent.getId(), "First release")).isPositive();
    }

    @Test
    void testPublishAgentWithUnpublishedSubAgentThrowsSubAgentNotPublished() {
        AiAgentDTO parentAgentDTO = agentFacade.createAgent("Parent Bot", null, workspaceId);
        AiAgentDTO subAgentDTO = agentFacade.createAgent("Sub Bot", null, workspaceId);

        AiAgent parentAgent = parentAgentDTO.agent();
        AiAgent subAgent = subAgentDTO.agent();

        agentFacade.addAgentElement(
            parentAgent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);
        agentFacade.addAgentElement(parentAgent.getId(), AiAgentElement.KIND_SUB_AGENT, subAgent.getId(), null, null);

        assertThatThrownBy(() -> agentFacade.publishAgent(parentAgent.getId(), "First release"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(AiAgentErrorType.SUB_AGENT_NOT_PUBLISHED.getErrorKey()));
    }

    @Test
    void testPublishAgentHappyPathReturnsVersionAndPublishesWorkflowWithTriggers() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        int newVersion = agentFacade.publishAgent(agent.getId(), "First release");

        assertThat(newVersion).isEqualTo(2);

        Project project = projectService.getProject(agent.getProjectId());
        ProjectVersion publishedVersion = project.getLastPublishedProjectVersion();

        assertThat(publishedVersion).isNotNull();
        assertThat(publishedVersion.getVersion()).isEqualTo(1);

        List<String> publishedWorkflowIds = projectWorkflowService.getProjectWorkflowIds(
            agent.getProjectId(), publishedVersion.getVersion());

        assertThat(publishedWorkflowIds).hasSize(1);

        Workflow publishedWorkflow = workflowService.getWorkflow(publishedWorkflowIds.get(0));

        assertThat(publishedWorkflow.getDefinition()).contains("\"name\":\"chat_1\"");
        assertThat(publishedWorkflow.getDefinition()).contains("\"name\":\"workflowCall_1\"");
    }

    @Test
    void testPublishAgentFlipsUnpublishedChangesThenSubsequentMutationFlipsItBack() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        assertThat(agentFacade.getAgent(agent.getId())
            .unpublishedChanges()).isTrue();

        agentFacade.publishAgent(agent.getId(), "First release");

        assertThat(agentFacade.getAgent(agent.getId())
            .unpublishedChanges()).isFalse();

        agentFacade.updateAgent(agent.getId(), "Support Bot", null, "Be nice");

        assertThat(agentFacade.getAgent(agent.getId())
            .unpublishedChanges()).isTrue();
    }

    @Test
    void testDeleteAgentAfterPublishRemovesWorkflowsAcrossAllVersions() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();
        long projectId = agent.getProjectId();

        agentFacade.addAgentElement(
            agent.getId(), AiAgentElement.KIND_MODEL, null, Map.of("provider", "openai", "model", "gpt-4"), null);

        agentFacade.publishAgent(agent.getId(), "First release");

        Project project = projectService.getProject(projectId);
        List<String> allWorkflowIds = new ArrayList<>();

        for (ProjectVersion projectVersion : project.getProjectVersions()) {
            allWorkflowIds.addAll(
                projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion.getVersion()));
        }

        // One workflow row for the published version (the frozen snapshot) plus one for the fresh draft version
        // (publishAgent's duplicate) — see testPublishAgentHappyPathReturnsVersionAndPublishesWorkflowWithTriggers.
        assertThat(allWorkflowIds).hasSize(2);

        agentFacade.deleteAgent(agent.getId());

        assertThat(projectRepository.findById(projectId)).isEmpty();

        for (String workflowId : allWorkflowIds) {
            assertThat(workflowCrudRepository.findById(workflowId)).isEmpty();
        }
    }

    /**
     * The channel definitions the client's cards, add menu and approval picker are built from. Asserts the two things
     * no compiler can: that the synthesized {@code schedule} entry — which no component declares, so the registry
     * cannot supply it — survives all the way to the facade, and that every entry carries a non-blank title, since the
     * client renders that string directly and a missing declaration would surface a raw lowercase component name.
     */
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

    /**
     * Pins the whole {@code getWorkspaceChatAgents} chain against real rows: an enabled deployment of the agent's
     * hidden project, an enabled deployment workflow, the generated draft's {@code chat/v1/newChatRequest} trigger
     * (whose {@code parameters.mode} is {@code 1} — the value {@code HostedChatTriggers.hasHostedChatTrigger} accepts),
     * and the resulting {@code workflowExecutionId}, asserted against an independently built
     * {@link WorkflowExecutionId} so a drift in its construction fails here rather than in the client.
     */
    @Test
    void testGetWorkspaceChatAgentsReturnsDeployedChatAgent() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        String workflowId = draftWorkflowId(agent.getProjectId());

        long projectDeploymentId = deploy(agent.getProjectId(), workflowId, true, true);

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
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        deploy(agent.getProjectId(), draftWorkflowId(agent.getProjectId()), false, true);

        stubStaticWebhookChatTriggerDefinition();

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    @Test
    void testGetWorkspaceChatAgentsSkipsDisabledDeploymentWorkflow() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        deploy(agent.getProjectId(), draftWorkflowId(agent.getProjectId()), true, false);

        stubStaticWebhookChatTriggerDefinition();

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    /**
     * The negative half of {@code HostedChatTriggers.hasHostedChatTrigger}: a deployed, enabled agent workflow whose
     * only trigger is not a {@code chat/} one must not surface as an openable chat, even though its trigger definition
     * resolves to a {@code STATIC_WEBHOOK}.
     */
    @Test
    void testGetWorkspaceChatAgentsSkipsWorkflowWithoutChatTrigger() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        String workflowId = draftWorkflowId(agent.getProjectId());

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

        deploy(agent.getProjectId(), workflowId, true, true);

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.DEVELOPMENT.ordinal())).isEmpty();
    }

    @Test
    void testGetWorkspaceChatAgentsSkipsOtherEnvironment() {
        AiAgentDTO agentDTO = agentFacade.createAgent("Support Bot", null, workspaceId);
        AiAgent agent = agentDTO.agent();

        deploy(agent.getProjectId(), draftWorkflowId(agent.getProjectId()), true, true);

        stubStaticWebhookChatTriggerDefinition();

        assertThat(agentFacade.getWorkspaceChatAgents(workspaceId, Environment.PRODUCTION.ordinal())).isEmpty();
    }

    /**
     * Saves an enabled/disabled {@link ProjectDeployment} of {@code projectId} in {@code DEVELOPMENT} plus a single
     * {@link ProjectDeploymentWorkflow} row pointing at {@code workflowId}, and returns the deployment's id.
     */
    private long deploy(
        long projectId, String workflowId, boolean deploymentEnabled, boolean deploymentWorkflowEnabled) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName("test-deployment");
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);
        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setEnabled(deploymentEnabled);
        projectDeployment.setUuid(UUID.randomUUID());

        ProjectDeployment savedProjectDeployment = projectDeploymentRepository.save(projectDeployment);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(savedProjectDeployment.getId());
        projectDeploymentWorkflow.setWorkflowId(workflowId);
        projectDeploymentWorkflow.setEnabled(deploymentWorkflowEnabled);

        projectDeploymentWorkflowRepository.save(projectDeploymentWorkflow);

        return savedProjectDeployment.getId();
    }

    /**
     * Makes the mocked {@code TriggerDefinitionService} resolve the generated draft's chat trigger the way the real
     * registry does — {@code STATIC_WEBHOOK}, name {@code newChatRequest} (i.e. not {@code manual}) — which is what
     * makes {@code resolveStaticWebhookExecutionId} produce a URL-bearing row.
     */
    private void stubStaticWebhookChatTriggerDefinition() {
        TriggerDefinition triggerDefinition = Mockito.mock(TriggerDefinition.class);

        Mockito.when(triggerDefinition.getType())
            .thenReturn(TriggerType.STATIC_WEBHOOK);
        Mockito.when(triggerDefinition.getName())
            .thenReturn("newChatRequest");

        Mockito.when(triggerDefinitionService.getTriggerDefinition("chat", 1, "newChatRequest"))
            .thenReturn(triggerDefinition);
    }

    private String draftDefinition(long projectId) {
        Workflow workflow = workflowService.getWorkflow(draftWorkflowId(projectId));

        return workflow.getDefinition();
    }

    private List<WorkflowTestConfigurationConnection> draftTestConnections(long projectId) {
        WorkflowTestConfiguration workflowTestConfiguration = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(draftWorkflowId(projectId), Environment.DEVELOPMENT.ordinal())
            .orElseThrow();

        return workflowTestConfiguration.getConnections();
    }

    private String draftWorkflowId(long projectId) {
        Project project = projectService.getProject(projectId);
        List<String> workflowIds = projectWorkflowService.getProjectWorkflowIds(
            projectId, project.getLastProjectVersion());

        assertThat(workflowIds).hasSize(1);

        return workflowIds.get(0);
    }
}

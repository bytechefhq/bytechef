/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserProjectFacadeIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserProjectFacadeIntTest {

    private static final String HIDDEN_PERMISSION_EXPRESSION = "connectedUser.metadata.plan == 'hidden'";

    private static final String COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION = """
        {"label":"Leads","inputs":[{"name":"sheetName","label":"Sheet","type":"string","required":false}],
         "triggers":[],"tasks":[]}
        """;

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Sync leads","description":"Posts new leads","triggers":[],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    @Autowired
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    // Publishing would otherwise run the rollout from the after-commit listener and catch every reference up to the
    // newly published version, which is exactly the pending update some tests here observe.
    @MockitoBean
    private AutomationWorkflowProjectPublishedEventListener automationWorkflowProjectPublishedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @MockitoBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    private String externalUserId;

    @BeforeEach
    void setUp() {
        externalUserId = createConnectedUser();

        when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
            .thenReturn(new ComponentDefinition("slack"));
        when(connectionService.getConnections(PlatformType.EMBEDDED))
            .thenReturn(List.of());
        when(environmentService.getEnvironment(Environment.PRODUCTION.ordinal()))
            .thenReturn(Environment.PRODUCTION);
        when(principalJobService.getJobIds(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                .thenReturn(Page.empty());

        // Every automation workflow project is visible unless it carries the hidden expression.
        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
        when(embeddedPermissionEvaluator.evaluate(eq(HIDDEN_PERMISSION_EXPRESSION), any()))
            .thenReturn(false);

        stubEntitledSlackConnection(777L);
    }

    @Test
    void testPrepareCopilotChatAllowsComponentsOfEnabledIntegrationsAndConnectionlessComponents() {
        String enabledComponentName = "enabled" + uniqueSuffix();
        String disabledComponentName = "disabled" + uniqueSuffix();

        createIntegrationInstanceConfiguration(enabledComponentName, true);
        createIntegrationInstanceConfiguration(disabledComponentName, false);

        ComponentDefinition loggerComponentDefinition = mock(ComponentDefinition.class);

        when(loggerComponentDefinition.getName()).thenReturn("logger");
        when(loggerComponentDefinition.isConnectionRequired()).thenReturn(false);

        ComponentDefinition httpClientComponentDefinition = mock(ComponentDefinition.class);

        when(httpClientComponentDefinition.getName()).thenReturn("httpClient");
        when(httpClientComponentDefinition.isConnectionRequired()).thenReturn(true);

        when(componentDefinitionService.getComponentDefinitions())
            .thenReturn(List.of(loggerComponentDefinition, httpClientComponentDefinition));

        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION, Environment.PRODUCTION);

        CopilotChatContextDTO copilotChatContextDTO = connectedUserProjectFacade.prepareCopilotChat(
            externalUserId, workflowUuid, Environment.PRODUCTION);

        assertThat(copilotChatContextDTO.allowedComponentNames())
            .contains(enabledComponentName, "logger")
            .doesNotContain(disabledComponentName, "httpClient");
    }

    @Test
    void testUpdateProjectWorkflowInputsWritesValuesToThePublishedDeploymentWorkflowNotTheDraft() {
        String workflowUuid = createPublishedCopy(externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, workflowUuid, Map.of("sheetName", "Leads"), null);

        long projectDeploymentId = getCopyProjectDeploymentId(externalUserId);
        String publishedWorkflowId = getPublishedCopyWorkflowId(externalUserId, workflowUuid);

        assertThat(publishedWorkflowId).isNotEqualTo(projectWorkflowService.getLastWorkflowId(workflowUuid));

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(projectDeploymentId, publishedWorkflowId);

        assertThat(projectDeploymentWorkflow.getInputs()).isEqualTo(Map.of("sheetName", "Leads"));
    }

    /**
     * The lookup runs against the connected user's OWN project, so another user's workflowUuid is simply absent from
     * it. That surfaces as not-found rather than as a permission error, which is what stops a connected user probing
     * uuids to learn which ones exist.
     */
    @Test
    void testUpdateProjectWorkflowInputsRefusesAWorkflowOutsideTheConnectedUsersProject() {
        createPublishedCopy(externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        String otherExternalUserId = createConnectedUser();

        String otherWorkflowUuid = createPublishedCopy(
            otherExternalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        assertThatThrownBy(() -> connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, otherWorkflowUuid, Map.of("sheetName", "Leads"), null))
                .isInstanceOf(ConfigurationException.class);

        ProjectDeploymentWorkflow otherProjectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(
                getCopyProjectDeploymentId(otherExternalUserId),
                getPublishedCopyWorkflowId(otherExternalUserId, otherWorkflowUuid));

        assertThat(otherProjectDeploymentWorkflow.getInputs()).isEmpty();
    }

    /**
     * A workflowUuid that belongs to a reference row is written to the reference's own deployment on the automation workflow project
     * project, never to the caller's copy-mode deployment -- the same reference-vs-copy branch
     * {@code enableProjectWorkflow} makes.
     */
    @Test
    void testUpdateProjectWorkflowInputsOnAReferenceWritesTheReferenceDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Reference Inputs");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, automationWorkflowUuid, Map.of("channel", "#alerts"), null);

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .singleElement()
            .satisfies(projectDeploymentWorkflow -> assertThat(projectDeploymentWorkflow.getInputs())
                .isEqualTo(Map.of("channel", "#alerts")));
    }

    @Test
    void testUpdateProjectWorkflowInputsOnADanglingReferenceIsRefused() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Dangling Inputs");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProject(automationWorkflowProjectId);

        assertThatThrownBy(() -> connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, automationWorkflowUuid, Map.of("channel", "#alerts"), null))
                .isInstanceOf(DanglingReferenceException.class);
    }

    @Test
    void testGetConnectedUserProjectWorkflowsListsAReferenceUnderItsAutomationWorkflowTemplate() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Reference List");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.kind())
                    .isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
                assertThat(connectedUserProjectWorkflowDTO.automationWorkflowUuid()).isEqualTo(automationWorkflowUuid);
                assertThat(connectedUserProjectWorkflowDTO.workflowUuid()).isEqualTo(automationWorkflowUuid);
                assertThat(connectedUserProjectWorkflowDTO.dangling()).isFalse();
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getLabel()).isEqualTo("Sync leads");
                assertThat(connectedUserProjectWorkflowDTO.components()).extracting("name")
                    .containsExactly("slack");
            });
    }

    @Test
    void testGetConnectedUserProjectWorkflowsFallsBackToTheUuidLabelWhenTheReferenceIsDangling() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Dangling List");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProject(automationWorkflowProjectId);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.kind())
                    .isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
                assertThat(connectedUserProjectWorkflowDTO.dangling()).isTrue();
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getLabel()).isEqualTo(automationWorkflowUuid);
                assertThat(connectedUserProjectWorkflowDTO.components()).isEmpty();
            });
    }

    @Test
    void testGetConnectedUserProjectWorkflowsReportsThePublishedLabelRatherThanTheDraftLabel() {
        String workflowUuid = createPublishedCopy(externalUserId, """
            {"label":"Published name","description":"Published description","triggers":[],"tasks":[]}
            """);

        connectedUserProjectFacade.updateProjectWorkflow(externalUserId, workflowUuid, """
            {"label":"Half-typed draft","description":"Draft description","triggers":[],"tasks":[]}
            """, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getLabel()).isEqualTo("Published name");
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getDescription()).isEqualTo("Published description");
            });
    }

    @Test
    void testGetConnectedUserProjectWorkflowsCarriesCopiedFromWorkflowUuidOnCopyRowsOnly() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Copy And Reference");

        String copiedTemplateUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String referencedTemplateUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserProjectFacade.copyWorkflowTemplate(externalUserId, copiedTemplateUuid, Environment.PRODUCTION);
        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, referencedTemplateUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .filteredOn(connectedUserProjectWorkflowDTO -> connectedUserProjectWorkflowDTO
                .kind() == ConnectedUserProjectWorkflowDTO.Kind.COPY)
            .singleElement()
            .satisfies(copyDTO -> assertThat(copyDTO.copiedFromWorkflowUuid()).isEqualTo(copiedTemplateUuid));
        assertThat(connectedUserProjectWorkflowDTOs)
            .filteredOn(connectedUserProjectWorkflowDTO -> connectedUserProjectWorkflowDTO
                .kind() == ConnectedUserProjectWorkflowDTO.Kind.REFERENCE)
            .singleElement()
            .satisfies(referenceDTO -> assertThat(referenceDTO.copiedFromWorkflowUuid()).isNull());
    }

    @Test
    void testGetConnectedUserProjectWorkflowsForwardsTheStoredInputsAndAttentionReason() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Attention");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, automationWorkflowUuid, Map.of("channel", "#alerts"), null);

        // A newer automation workflow project version the reference has not caught up with yet.
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.attentionReason())
                    .isEqualTo(ConnectedUserReferenceAttentionResolver.UPDATE_PENDING);
                assertThat(connectedUserProjectWorkflowDTO.inputValues()).isEqualTo(Map.of("channel", "#alerts"));
            });
    }

    @Test
    void testEnableProjectWorkflowOnAReferenceUuidTogglesTheReference() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Enable Reference");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationWorkflowUuid, false, null);

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationWorkflowUuid, true, null);

        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
    }

    /**
     * Regression pin: a copy-mode uuid (no matching reference row) keeps going through the connected user's own project
     * deployment.
     */
    @Test
    void testEnableProjectWorkflowOnACopyUuidTogglesTheConnectedUsersDeploymentWorkflow() {
        String workflowUuid = createPublishedCopy(externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        long projectDeploymentId = getCopyProjectDeploymentId(externalUserId);
        String publishedWorkflowId = getPublishedCopyWorkflowId(externalUserId, workflowUuid);

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, false, null);

        assertThat(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, publishedWorkflowId)).isFalse();

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, true, null);

        assertThat(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, publishedWorkflowId)).isTrue();
    }

    @Test
    void testEnableProjectWorkflowByIdOnAReferenceRowTogglesTheReference() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Enable Reference Row");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.enableProjectWorkflow(reference.getId(), false);

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
    }

    @Test
    void testDeleteProjectWorkflowByIdOnAReferenceRowDeletesTheReferenceAndItsDeployment() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Reference Row");

        String automationWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationWorkflowUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.deleteProjectWorkflow(reference.getId());

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isEmpty();
    }

    @Test
    void testCopyWorkflowTemplateCopiesATemplateTheConnectedUserIsPermittedToSee() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Visible Template");

        String visibleWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        createPublishedHiddenAutomationWorkflowTemplate("Hidden Sibling");

        String copyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, visibleWorkflowUuid, Environment.PRODUCTION);

        assertThat(copyWorkflowUuid).isNotEqualTo(visibleWorkflowUuid);
        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .singleElement()
            .satisfies(copyDTO -> {
                assertThat(copyDTO.workflowUuid()).isEqualTo(copyWorkflowUuid);
                assertThat(copyDTO.copiedFromWorkflowUuid()).isEqualTo(visibleWorkflowUuid);
            });
    }

    @Test
    void testCopyWorkflowTemplateRejectsATemplateHiddenByThePermissionExpression() {
        String hiddenWorkflowUuid = createPublishedHiddenAutomationWorkflowTemplate("Hidden Template");

        assertThatThrownBy(() -> connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, hiddenWorkflowUuid, Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .isEmpty();
    }

    /**
     * The point of the rejection: a caller must not be able to tell a template that exists but is hidden from them
     * apart from a uuid that does not exist at all. Same exception type, and a message that differs only by the uuid
     * the caller itself supplied.
     */
    @Test
    void testHiddenTemplateRejectionIsIndistinguishableFromAnUnknownUuid() {
        String hiddenWorkflowUuid = createPublishedHiddenAutomationWorkflowTemplate("Indistinguishable");

        String unknownWorkflowUuid = UUID.randomUUID()
            .toString();

        Throwable hiddenThrowable = catchThrowable(() -> connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, hiddenWorkflowUuid, Environment.PRODUCTION));
        Throwable unknownThrowable = catchThrowable(() -> connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, unknownWorkflowUuid, Environment.PRODUCTION));

        assertThat(hiddenThrowable).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(unknownThrowable).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(hiddenThrowable).hasMessage(
            "Not a published automation workflow template: " + hiddenWorkflowUuid);
        assertThat(unknownThrowable).hasMessage(
            "Not a published automation workflow template: " + unknownWorkflowUuid);
    }

    private String addAutomationWorkflow(long automationWorkflowProjectId, String definition) {
        String workflowId = automationWorkflowProjectFacade.createProjectWorkflow(automationWorkflowProjectId, definition, null);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return projectWorkflow.getUuidAsString();
    }

    private long createAutomationWorkflowProject(String name) {
        return createAutomationWorkflowProject(name, null);
    }

    private long createAutomationWorkflowProject(String name, String permissionExpression) {
        return automationWorkflowProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), permissionExpression, null);
    }

    private String createPublishedHiddenAutomationWorkflowTemplate(String name) {
        long automationWorkflowProjectId = createAutomationWorkflowProject(name, HIDDEN_PERMISSION_EXPRESSION);

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        return workflowUuid;
    }

    private String createConnectedUser() {
        String newExternalUserId = "project-facade-user-" + UUID.randomUUID();

        connectedUserService.createConnectedUser(newExternalUserId, Environment.PRODUCTION);

        return newExternalUserId;
    }

    private void createIntegrationInstanceConfiguration(String componentName, boolean enabled) {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setName(componentName);

        integration = integrationService.create(integration);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(componentName);

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), enabled);
    }

    private String createPublishedCopy(String copyExternalUserId, String definition) {
        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            copyExternalUserId, definition, Environment.PRODUCTION);

        connectedUserProjectFacade.publishProjectWorkflow(copyExternalUserId, workflowUuid, "published", null);

        return workflowUuid;
    }

    private long getCopyProjectDeploymentId(String copyExternalUserId) {
        long projectId = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(copyExternalUserId, Environment.PRODUCTION)
            .getProjectId();

        return projectDeploymentService.getProjectDeploymentId(projectId, Environment.PRODUCTION);
    }

    private String getPublishedCopyWorkflowId(String copyExternalUserId, String workflowUuid) {
        return projectWorkflowService
            .fetchProjectWorkflowWorkflowId(getCopyProjectDeploymentId(copyExternalUserId), workflowUuid)
            .orElseThrow();
    }

    private ConnectedUserProjectWorkflow getReference(long id) {
        return connectedUserProjectWorkflowRepository.findById(id)
            .orElseThrow();
    }

    private void stubEntitledSlackConnection(long connectionId) {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);
        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);

        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

        when(connectionDTO.id()).thenReturn(connectionId);
        when(connectionDTO.componentName()).thenReturn("slack");
        when(connectedUserConnectionFacade.getConnections(anyLong(), eq("slack"), eq(List.of())))
            .thenReturn(List.of(connectionDTO));
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class))).thenReturn(List.of(slot));
        when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
            .thenReturn(slot);
        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }

    private static String uniqueSuffix() {
        return UUID.randomUUID()
            .toString()
            .replace("-", "");
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }
}

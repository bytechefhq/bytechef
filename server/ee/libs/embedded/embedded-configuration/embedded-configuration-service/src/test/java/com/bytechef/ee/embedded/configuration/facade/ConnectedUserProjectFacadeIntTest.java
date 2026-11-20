/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationRowFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRowFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.InvalidWorkflowException;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.evaluator.SpelEvaluator;
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
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
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
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpAutomationRowFacade.class, McpIntegrationInstanceRowFacade.class,
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

    private static final int CONCURRENT_ROUND_COUNT = 20;

    private static final String BROKEN_COPY_WORKFLOW_DEFINITION = """
        {"label":"Broken leads","inputs":[],"triggers":[],"tasks":[]}
        """;

    private static final String BROKEN_SLACK_WORKFLOW_DEFINITION = """
        {"label":"Broken post","description":"Posts without a channel","triggers":[],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION = """
        {"label":"Leads","inputs":[{"name":"sheetName","label":"Sheet","type":"string","required":false}],
         "triggers":[],"tasks":[]}
        """;

    private static final String COPY_WORKFLOW_WITH_SHEET_AND_INTERNAL_ONLY_INPUTS_DEFINITION = """
        {"label":"Leads","inputs":[{"name":"sheetName","label":"Sheet","type":"string","required":false},
         {"name":"accountId","label":"Account","type":"string","required":false,"internalOnly":true}],
         "triggers":[],"tasks":[]}
        """;

    private static final String MANUAL_SLACK_WORKFLOW_DEFINITION = """
        {"label":"Post manually","description":"Posts on demand",
         "triggers":[{"label":"Manual","name":"trigger_1","type":"manual/v1/manual"}],
         "tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
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
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @Autowired
    private AutomationProjectFacade automationProjectFacade;

    @MockitoBean
    private AutomationProjectConfigurationUpdatedEventListener automationProjectConfigurationUpdatedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @MockitoSpyBean
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

    @Autowired
    private ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @MockitoSpyBean
    private ConnectedUserService connectedUserService;

    @MockitoSpyBean
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionService connectionService;

    @MockitoBean
    private CopilotWorkflowGenerator copilotWorkflowGenerator;

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private JobService jobService;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Autowired
    private WorkflowValidatorFacade workflowValidatorFacade;

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

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(true);
        when(embeddedPermissionEvaluator.evaluate(eq(HIDDEN_PERMISSION_EXPRESSION), any()))
            .thenReturn(false);

        stubEntitledSlackConnection(777L);

        when(workflowValidatorFacade.validateWorkflow(
            argThat(definition -> definition != null && definition.contains("\"Broken ")), anyLong()))
                .thenReturn(
                    new WorkflowValidationResult(
                        List.of("[postMessage1] Missing required property: channel"),
                        List.of("[postMessage1] Property 'text' might not exist")));
    }

    @Test
    void testEnableProjectWorkflowOnACopyWhosePublishedWorkflowHasErrorsIsRefused() {
        String workflowUuid = createPublishedCopy(externalUserId, BROKEN_COPY_WORKFLOW_DEFINITION);

        long projectDeploymentId = getCopyProjectDeploymentId(externalUserId);
        String publishedWorkflowId = getPublishedCopyWorkflowId(externalUserId, workflowUuid);

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, false, null);

        assertThatThrownBy(
            () -> connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, true, null))
                .isInstanceOfSatisfying(
                    InvalidWorkflowException.class,
                    invalidWorkflowException -> assertThat(invalidWorkflowException.getErrorCount()).isEqualTo(1));

        assertThat(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, publishedWorkflowId)).isFalse();
    }

    @Test
    void testEnableProjectWorkflowOnACopyEnablesAValidPublishedVersionDespiteABrokenDraft() {
        String workflowUuid = createPublishedCopy(externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        connectedUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowUuid, BROKEN_COPY_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        long projectDeploymentId = getCopyProjectDeploymentId(externalUserId);
        String publishedWorkflowId = getPublishedCopyWorkflowId(externalUserId, workflowUuid);

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, false, null);
        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, workflowUuid, true, null);

        assertThat(projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, publishedWorkflowId)).isTrue();
    }

    @Test
    void testUpdateProjectWorkflowInputsIgnoresAnInternalOnlyInputOnACopy() {
        String workflowUuid = createPublishedCopy(
            externalUserId, COPY_WORKFLOW_WITH_SHEET_AND_INTERNAL_ONLY_INPUTS_DEFINITION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, workflowUuid, Map.of("sheetName", "Leads", "accountId", "user-account"), null);

        ProjectDeploymentWorkflow row = projectDeploymentWorkflowService.getProjectDeploymentWorkflow(
            getCopyProjectDeploymentId(externalUserId), getPublishedCopyWorkflowId(externalUserId, workflowUuid));

        assertThat(row.getInputs()).isEqualTo(Map.of("sheetName", "Leads"));
    }

    @Test
    void testEnableProjectWorkflowOnAReferenceWhoseCatalogWorkflowHasErrorsIsRefused() {
        long automationProjectId = createAutomationProject("Broken Reference");

        String automationUuid = addAutomation(
            automationProjectId, BROKEN_SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();

        assertThatThrownBy(
            () -> connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationUuid, true, null))
                .isInstanceOfSatisfying(
                    InvalidWorkflowException.class,
                    invalidWorkflowException -> assertThat(invalidWorkflowException.getErrorCount()).isEqualTo(1));

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
    }

    @Test
    void testGetConnectedUserProjectWorkflowsReportsAnInvalidWorkflowOnACopyAndOnAReference() {
        createPublishedCopy(externalUserId, BROKEN_COPY_WORKFLOW_DEFINITION);

        long automationProjectId = createAutomationProject("Broken Listed Reference");

        String automationUuid = addAutomation(
            automationProjectId, BROKEN_SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .hasSize(2)
            .allSatisfy(connectedUserProjectWorkflowDTO -> assertThat(connectedUserProjectWorkflowDTO.attentionReason())
                .isEqualTo(ConnectedUserReferenceAttentionResolver.INVALID_WORKFLOW_PREFIX + 1));
    }

    @Test
    void testGetConnectedUserProjectWorkflowsReportsNoInvalidWorkflowForAValidPublishedCopyWithABrokenDraft() {
        String workflowUuid = createPublishedCopy(externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        connectedUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowUuid, BROKEN_COPY_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> assertThat(connectedUserProjectWorkflowDTO.attentionReason())
                .isNull());
    }

    @Test
    void testGetConnectedUserProjectWorkflowsReportsTheLastExecutionOfThePublishedVersionBehindADraft() {
        String workflowUuid = createPublishedCopy(externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION);

        connectedUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowUuid, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION, Environment.PRODUCTION);

        String publishedWorkflowId = getPublishedCopyWorkflowId(externalUserId, workflowUuid);

        Instant endDate = Instant.parse("2026-11-15T00:28:50Z");

        Job job = new Job(1L);

        job.setEndDate(endDate);

        when(jobService.fetchLastWorkflowJob(argThat((List<String> workflowIds) -> workflowIds.contains(
            publishedWorkflowId)))).thenReturn(Optional.of(job));

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .singleElement()
            .extracting(ConnectedUserProjectWorkflowDTO::lastExecutionDate)
            .isEqualTo(endDate);
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
    void testCreateProjectWorkflowFromPromptDeletesTheDraftWhenGenerationFails() {
        IllegalStateException generationFailure = new IllegalStateException(
            "Workflow generation failed: Your credit balance is too low");

        doThrow(generationFailure)
            .when(copilotWorkflowGenerator)
            .generateWorkflow(anyString(), anyLong(), anyLong(), anyString(), any(), any());

        assertThatThrownBy(() -> connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, "Post new leads to Slack", null, Environment.PRODUCTION, true))
                .isSameAs(generationFailure);

        long projectId = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(externalUserId, Environment.PRODUCTION)
            .getProjectId();

        assertThat(projectWorkflowService.getProjectWorkflows(projectId)).isEmpty();
    }

    @Test
    void testUpdateProjectWorkflowFromPromptPassesTheSystemPromptToTheGenerator() {
        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, COPY_WORKFLOW_WITH_SHEET_INPUT_DEFINITION, Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflow(
            externalUserId, workflowUuid, "Post new leads to Slack", "Use the sales channel", Environment.PRODUCTION,
            true);

        verify(copilotWorkflowGenerator).generateWorkflow(
            eq(projectWorkflowService.getLastWorkflowId(workflowUuid)), anyLong(), anyLong(),
            eq("Post new leads to Slack"), eq("Use the sales channel"), any());
    }

    @Test
    void testPublishProjectWorkflowLeavesOutAnotherConnectedUsersConnection() {
        String otherExternalUserId = createConnectedUser();

        long otherConnectionId = createOwnedConnection(otherExternalUserId);

        List<ProjectDeploymentWorkflowConnection> connections = publishWithTestConnection(otherConnectionId);

        assertThat(connections).isEmpty();
    }

    @Test
    void testPublishProjectWorkflowDeploysTheConnectedUsersOwnConnection() {
        long ownConnectionId = createOwnedConnection(externalUserId);

        List<ProjectDeploymentWorkflowConnection> connections = publishWithTestConnection(ownConnectionId);

        assertThat(connections)
            .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
            .containsExactly(ownConnectionId);
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

    @Test
    void testUpdateProjectWorkflowInputsOnAReferenceWritesTheReferenceDeployment() {
        long automationProjectId = createAutomationProject("Reference Inputs");

        String automationUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, automationUuid, Map.of("channel", "#alerts"), null);

        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .singleElement()
            .satisfies(projectDeploymentWorkflow -> assertThat(projectDeploymentWorkflow.getInputs())
                .isEqualTo(Map.of("channel", "#alerts")));
    }

    @Test
    void testGetConnectedUserProjectWorkflowsReportsTheLastExecutionOfAReferenceOnItsOwnDeployment() {
        long automationProjectId = createAutomationProject("Reference Last Execution");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        long projectDeploymentId = reference.getProjectDeploymentId();

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId);

        String deployedWorkflowId = projectDeploymentWorkflows.getFirst()
            .getWorkflowId();

        Instant endDate = Instant.parse("2026-11-19T14:50:46Z");

        Job job = new Job(4242L);

        job.setEndDate(endDate);

        when(principalJobService.fetchLastWorkflowJobId(
            eq(projectDeploymentId), argThat((List<String> workflowIds) -> workflowIds.contains(deployedWorkflowId)),
            eq(PlatformType.AUTOMATION)))
                .thenReturn(Optional.of(4242L));
        when(jobService.getJob(4242L))
            .thenReturn(job);

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .singleElement()
            .extracting(ConnectedUserProjectWorkflowDTO::lastExecutionDate)
            .isEqualTo(endDate);
    }

    @Test
    void testUpdateProjectWorkflowInputsOnADanglingReferenceIsRefused() {
        long automationProjectId = createAutomationProject("Dangling Inputs");

        String automationUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProject(automationProjectId);

        assertThatThrownBy(() -> connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, automationUuid, Map.of("channel", "#alerts"), null))
                .isInstanceOf(DanglingReferenceException.class);
    }

    @Test
    void testGetConnectedUserProjectWorkflowsListsTheTriggersOfAReferencedAutomation() {
        long automationProjectId = createAutomationProject("Reference Triggers");

        String automationUuid = addAutomation(automationProjectId, MANUAL_SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> assertThat(connectedUserProjectWorkflowDTO.workflow()
                .getTriggers())
                    .extracting("type")
                    .containsExactly("manual/v1/manual"));
    }

    @Test
    void testGetConnectedUserProjectWorkflowsListsAReferenceUnderItsAutomationTemplate() {
        long automationProjectId = createAutomationProject("Reference List");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.kind())
                    .isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
                assertThat(connectedUserProjectWorkflowDTO.automationUuid()).isEqualTo(automationUuid);
                assertThat(connectedUserProjectWorkflowDTO.workflowUuid()).isEqualTo(automationUuid);
                assertThat(connectedUserProjectWorkflowDTO.dangling()).isFalse();
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getLabel()).isEqualTo("Sync leads");
                assertThat(connectedUserProjectWorkflowDTO.components()).extracting("name")
                    .containsExactly("slack");
            });
    }

    @Test
    void testGetConnectedUserProjectWorkflowsKeepsTheLabelOfAReferenceTurnedOffByTheAdministrator() {
        long automationProjectId = createAutomationProject("Turned Off");

        String automationUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateEnabled(configuration.getId(), false);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.attentionReason())
                    .isEqualTo(ConnectedUserReferenceAttentionResolver.TURNED_OFF_BY_ADMINISTRATOR);
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getLabel()).isEqualTo("Post");
                assertThat(connectedUserProjectWorkflowDTO.components()).extracting("name")
                    .containsExactly("slack");
                assertThat(connectedUserProjectWorkflowDTO.inputs()).extracting("name")
                    .containsExactly("channel");
            });
    }

    @Test
    void testGetConnectedUserProjectsListsAReferenceOfAConnectedUserWhoseProjectIsNotDeployed() {
        long automationProjectId = createAutomationProject("Reference Only");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectDTO> connectedUserProjectDTOs = connectedUserProjectFacade.getConnectedUserProjects(
            externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectDTOs)
            .singleElement()
            .satisfies(connectedUserProjectDTO -> {
                assertThat(connectedUserProjectDTO.environment()).isEqualTo(Environment.PRODUCTION);
                assertThat(connectedUserProjectDTO.projectVersion()).isNull();
                assertThat(connectedUserProjectDTO.lastExecutionDate()).isNull();
                assertThat(connectedUserProjectDTO.connectedUserProjectWorkflows())
                    .singleElement()
                    .satisfies(connectedUserProjectWorkflowDTO -> {
                        assertThat(connectedUserProjectWorkflowDTO.kind())
                            .isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
                        assertThat(connectedUserProjectWorkflowDTO.automationUuid())
                            .isEqualTo(automationUuid);
                    });
            });
    }

    @Test
    void testGetConnectedUserProjectsLeavesOutConnectedUsersOfAnotherEnvironment() {
        long automationProjectId = createAutomationProject("Reference Other Environment");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        Long connectedUserId = connectedUser.getId();

        assertThat(connectedUserProjectFacade.getConnectedUserProjects(connectedUserId, Environment.DEVELOPMENT))
            .isEmpty();
        assertThat(connectedUserProjectFacade.getConnectedUserProjects(connectedUserId, null))
            .hasSize(1);
    }

    @Test
    void testGetConnectedUserProjectWorkflowsFallsBackToTheUuidLabelWhenTheReferenceIsDangling() {
        long automationProjectId = createAutomationProject("Dangling List");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        automationProjectFacade.deleteProject(automationProjectId);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> {
                assertThat(connectedUserProjectWorkflowDTO.kind())
                    .isEqualTo(ConnectedUserProjectWorkflowDTO.Kind.REFERENCE);
                assertThat(connectedUserProjectWorkflowDTO.dangling()).isTrue();
                assertThat(connectedUserProjectWorkflowDTO.workflow()
                    .getLabel()).isEqualTo(automationUuid);
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
        long automationProjectId = createAutomationProject("Copy And Reference");

        String copiedTemplateUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String referencedTemplateUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

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
    void testCopyAssignsAndUsesTheAssignedVersion() {
        long automationProjectId = createAutomationProject("Copy Assignment");

        String firstWorkflowTemplateUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String secondWorkflowTemplateUuid = addAutomation(
            automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        AutomationProjectConfiguration configurationA = automationProjectConfigurationService.create(
            automationProjectId, Environment.PRODUCTION.ordinal(), "A", null, 1, true, true);

        connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, firstWorkflowTemplateUuid, Environment.PRODUCTION);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(
            connectedUser.getId(), automationProjectId))
                .map(ConnectedUserProjectConfiguration::getConfigurationId)
                .contains(configurationA.getId());

        automationProjectFacade.updateProjectWorkflow(secondWorkflowTemplateUuid, "Version two", "");

        automationProjectFacade.publishProject(automationProjectId);

        automationProjectConfigurationService.create(
            automationProjectId, Environment.PRODUCTION.ordinal(), "B", null, 2, true, true);

        String secondCopyWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, secondWorkflowTemplateUuid, Environment.PRODUCTION);

        String secondCopyWorkflowId = projectWorkflowService.getLastWorkflowId(secondCopyWorkflowUuid);

        assertThat(workflowService.getWorkflow(secondCopyWorkflowId)
            .getLabel()).isEqualTo("Sync leads");
    }

    @Test
    void testConcurrentCopyAndProvisionForOneUserDoNotDeadlock() throws Exception {
        long automationProjectId = createAutomationProject("Concurrent Copy And Provision");

        String copiedWorkflowTemplateUuid = addAutomation(
            automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String referencedWorkflowTemplateUuid = addAutomation(
            automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        for (int round = 0; round < CONCURRENT_ROUND_COUNT; round++) {
            String roundExternalUserId = createConnectedUser();

            connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
                roundExternalUserId, Environment.PRODUCTION);

            CountDownLatch startLatch = new CountDownLatch(1);
            ExecutorService executorService = Executors.newFixedThreadPool(2);

            try {
                Future<?> copyFuture = executorService.submit(() -> {
                    awaitLatch(startLatch);

                    return connectedUserProjectFacade.copyWorkflowTemplate(
                        roundExternalUserId, copiedWorkflowTemplateUuid, Environment.PRODUCTION);
                });
                Future<?> provisionFuture = executorService.submit(() -> {
                    awaitLatch(startLatch);

                    return connectedUserWorkflowReferenceFacade.getOrCreateReference(
                        roundExternalUserId, referencedWorkflowTemplateUuid, Environment.PRODUCTION);
                });

                startLatch.countDown();

                assertThatCode(() -> copyFuture.get(60, TimeUnit.SECONDS)).doesNotThrowAnyException();
                assertThatCode(() -> provisionFuture.get(60, TimeUnit.SECONDS)).doesNotThrowAnyException();
            } finally {
                startLatch.countDown();

                executorService.shutdownNow();
            }
        }
    }

    @Test
    void testCopyWorkflowTemplateTwiceIsRefusedWithTheUuidOfTheExistingCopy() {
        long automationProjectId = createAutomationProject("Copy Twice");

        String workflowTemplateUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        String copiedWorkflowUuid = connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, workflowTemplateUuid, Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, workflowTemplateUuid, Environment.PRODUCTION))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> {
                    assertThat(configurationException.getErrorKey()).isEqualTo(
                        ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_ALREADY_COPIED.getErrorKey());
                    assertThat(configurationException.getInputParameters()
                        .get("copiedWorkflowUuid")).isEqualTo(copiedWorkflowUuid);
                });
    }

    @Test
    void testGetConnectedUserProjectWorkflowsForwardsTheStoredInputsAndAttentionReason() {
        long automationProjectId = createAutomationProject("Attention");

        String automationUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.updateProjectWorkflowInputs(
            externalUserId, automationUuid, Map.of("channel", "#alerts"), null);

        publishAndPinProject(automationProjectId);

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
    void testGetConnectedUserProjectWorkflowsReturnsTheConnectionEachComponentOfAReferenceUses() {
        long automationProjectId = createAutomationProject("Connections");

        String automationUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflowDTOs =
            connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowDTOs)
            .singleElement()
            .satisfies(connectedUserProjectWorkflowDTO -> assertThat(connectedUserProjectWorkflowDTO.connections())
                .isEqualTo(Map.of("slack", 777L)));
    }

    @Test
    void testEnableProjectWorkflowOnAReferenceTurnedOffByTheAdministratorReturnsTheAttentionReason() {
        long automationProjectId = createAutomationProject("Enable Turned Off");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateEnabled(configuration.getId(), false);

        assertThat(connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationUuid, true, null))
            .isEqualTo(ConnectedUserReferenceAttentionResolver.TURNED_OFF_BY_ADMINISTRATOR);
    }

    @Test
    void testEnableProjectWorkflowOnAReferenceReturnsNoAttentionReasonWhenNothingBlocksIt() {
        long automationProjectId = createAutomationProject("Enable Without Attention");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        assertThat(connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationUuid, true, null))
            .isNull();
        assertThat(
            connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationUuid, false, null))
                .isNull();
    }

    @Test
    void testEnableProjectWorkflowOnAReferenceUuidTogglesTheReference() {
        long automationProjectId = createAutomationProject("Enable Reference");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationUuid, false, null);

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();

        connectedUserProjectFacade.enableProjectWorkflow(externalUserId, automationUuid, true, null);

        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
    }

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
        long automationProjectId = createAutomationProject("Enable Reference Row");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.enableProjectWorkflow(reference.getId(), false);

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
    }

    @Test
    void testDeleteProjectWorkflowByIdOnAReferenceRowDeletesTheReferenceAndItsDeployment() {
        long automationProjectId = createAutomationProject("Delete Reference Row");

        String automationUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, automationUuid, Environment.PRODUCTION);

        connectedUserProjectFacade.deleteProjectWorkflow(reference.getId());

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isEmpty();
    }

    @Test
    void testCopyWorkflowTemplateCopiesATemplateTheConnectedUserIsPermittedToSee() {
        long automationProjectId = createAutomationProject("Visible Template");

        String visibleWorkflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        addHiddenAutomation(automationProjectId);

        publishAndPinProject(automationProjectId);

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
        long automationProjectId = createAutomationProject("Hidden Template");

        addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        String hiddenWorkflowUuid = addHiddenAutomation(automationProjectId);

        publishAndPinProject(automationProjectId);

        assertThatThrownBy(() -> connectedUserProjectFacade.copyWorkflowTemplate(
            externalUserId, hiddenWorkflowUuid, Environment.PRODUCTION))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(connectedUserProjectFacade.getConnectedUserProjectWorkflows(externalUserId, Environment.PRODUCTION))
            .isEmpty();
    }

    @Test
    void testHiddenTemplateRejectionIsIndistinguishableFromAnUnknownUuid() {
        long automationProjectId = createAutomationProject("Indistinguishable");

        String hiddenWorkflowUuid = addHiddenAutomation(automationProjectId);

        publishAndPinProject(automationProjectId);

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

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the start signal");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    private String addAutomation(long automationProjectId, String definition) {
        return addAutomation(automationProjectId, definition, null);
    }

    private String
        addAutomation(long automationProjectId, String definition, String permissionExpression) {
        return automationProjectFacade.createProjectWorkflow(
            automationProjectId, definition, permissionExpression);
    }

    private String addHiddenAutomation(long automationProjectId) {
        return addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION,
            HIDDEN_PERMISSION_EXPRESSION);
    }

    private long createAutomationProject(String name) {
        return automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private long createOwnedConnection(String connectionExternalUserId) {
        UUID connectionUuid = UUID.randomUUID();

        long connectionId = connectionUuid.getMostSignificantBits() & Long.MAX_VALUE;

        long connectedUserId = connectedUserService.getConnectedUser(connectionExternalUserId, Environment.PRODUCTION)
            .getId();

        connectedUserConnectionService.create(connectedUserId, connectionId, null);

        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        when(connectionService.getConnections(argThat((List<Long> connectionIds) -> connectionIds != null &&
            connectionIds.contains(connectionId))))
                .thenReturn(List.of(connection));
        when(connectionService.getConnection(connectionId)).thenReturn(connection);

        return connectionId;
    }

    private List<ProjectDeploymentWorkflowConnection> publishWithTestConnection(long connectionId) {
        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, SLACK_WORKFLOW_DEFINITION, Environment.PRODUCTION);

        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        WorkflowTestConfiguration workflowTestConfiguration = new WorkflowTestConfiguration();

        workflowTestConfiguration.setConnections(
            List.of(new WorkflowTestConfigurationConnection(connectionId, "slack", "postMessage1")));
        workflowTestConfiguration.setEnvironmentId(Environment.PRODUCTION.ordinal());
        workflowTestConfiguration.setWorkflowId(workflowId);

        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(
            workflowId, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(workflowTestConfiguration));

        connectedUserProjectFacade.publishProjectWorkflow(externalUserId, workflowUuid, "published", null);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflow(
                getCopyProjectDeploymentId(externalUserId), getPublishedCopyWorkflowId(externalUserId, workflowUuid));

        return projectDeploymentWorkflow.getConnections();
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
        doReturn(List.of(connectionDTO))
            .when(connectedUserConnectionFacade)
            .getConnections(anyLong(), eq("slack"), eq(List.of()));
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

    @Nested
    class Provisioning {

        @BeforeEach
        void setUpProvisioning() {
            reset(connectedUserConnectionFacade);
        }

        private static final AtomicLong NEXT_CONNECTION_ID = new AtomicLong(1000L);

        private static final String CHANNEL_INPUT_WORKFLOW_DEFINITION = """
            {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
             "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
            """;

        private static final String NO_CONNECTION_WORKFLOW_DEFINITION = """
            {"label":"Standalone","triggers":[],"tasks":[]}
            """;

        private static final String SLACK_WORKFLOW_DEFINITION =
            """
                {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
                """;

        @MockitoSpyBean
        private ConnectedUserProjectService connectedUserProjectService;

        @Autowired
        private AutomationProjectConfigurationFacade automationProjectConfigurationFacade;

        @Autowired
        private ConnectionFacade connectionFacade;

        private final Map<Long, Connection> embeddedConnections = new ConcurrentHashMap<>();

        @Test
        void testGetProvisionableProjectsListsReleasedProjectsWithTheirEnabledWorkflows() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithWorkflows("Provisionable Listed", 2);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            AutomationProjectDTO publishedProject = findPublishedProject(connectedUser, projectId);

            List<ProvisionableAutomationProjectDTO> result =
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId());

            ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(result, projectId);

            assertThat(provisionableProject.name()).isEqualTo(publishedProject.name());
            assertThat(provisionableProject.projectVersion()).isEqualTo(publishedProject.lastPublishedVersion());
            assertThat(provisionableProject.workflows())
                .extracting(
                    ProvisionableAutomationProjectDTO.Workflow::workflowUuid,
                    ProvisionableAutomationProjectDTO.Workflow::label,
                    ProvisionableAutomationProjectDTO.Workflow::inputs,
                    ProvisionableAutomationProjectDTO.Workflow::provisioned,
                    ProvisionableAutomationProjectDTO.Workflow::dangling)
                .containsExactlyInAnyOrderElementsOf(
                    publishedProject.workflowTemplates()
                        .stream()
                        .map(template -> tuple(
                            template.workflowUuid(), template.label(), template.inputs(), false, false))
                        .toList());
            assertThat(provisionableProject.workflows()).hasSize(2);
        }

        @Test
        void testGetProvisionableProjectsSkipsProjectsWithoutConfigurationInTheUserEnvironment() {
            givenSlackAutomationEnvironment();

            long unconfiguredProjectId = createProjectWithWorkflows("Provisionable Unconfigured", 1);

            automationProjectFacade.publishProject(unconfiguredProjectId);

            long configuredProjectId = createProjectWithWorkflows("Provisionable Configured", 1);

            publishAndPinProject(configuredProjectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            List<ProvisionableAutomationProjectDTO> result =
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId());

            assertThat(result)
                .extracting(ProvisionableAutomationProjectDTO::projectId)
                .contains(configuredProjectId)
                .doesNotContain(unconfiguredProjectId);
        }

        @Test
        void testGetProvisionableProjectsSkipsWorkflowsTurnedOffInTheConfiguration() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithWorkflows("Provisionable Turned Off", 2);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
                .workflowTemplates();

            String turnedOffWorkflowUuid = templates.getFirst()
                .workflowUuid();
            String remainingWorkflowUuid = templates.getLast()
                .workflowUuid();

            AutomationProjectConfiguration configuration = automationProjectConfigurationService
                .getConfigurations(projectId, Environment.PRODUCTION.ordinal())
                .stream()
                .findFirst()
                .orElseThrow();

            automationProjectConfigurationFacade.enableConfigurationWorkflow(
                configuration.getId(), turnedOffWorkflowUuid, false);

            ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId);

            assertThat(provisionableProject.workflows())
                .extracting(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
                .containsExactly(remainingWorkflowUuid);
        }

        @Test
        void testGetProvisionableProjectsMarksProvisionedAndDanglingWorkflows() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithWorkflows("Provisionable Marked", 3);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
                .workflowTemplates();

            String provisionedWorkflowUuid = templates.get(0)
                .workflowUuid();
            String danglingWorkflowUuid = templates.get(1)
                .workflowUuid();
            String untouchedWorkflowUuid = templates.get(2)
                .workflowUuid();

            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                connectedUser.getExternalId(), provisionedWorkflowUuid, Environment.PRODUCTION);

            ConnectedUserProjectWorkflow danglingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                connectedUser.getExternalId(), danglingWorkflowUuid, Environment.PRODUCTION);

            markDangling(danglingReference);

            ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId);

            assertThat(provisionableProject.workflows())
                .extracting(
                    ProvisionableAutomationProjectDTO.Workflow::workflowUuid,
                    ProvisionableAutomationProjectDTO.Workflow::provisioned,
                    ProvisionableAutomationProjectDTO.Workflow::dangling)
                .containsExactlyInAnyOrder(
                    tuple(provisionedWorkflowUuid, true, false),
                    tuple(danglingWorkflowUuid, true, true),
                    tuple(untouchedWorkflowUuid, false, false));
        }

        @Test
        void testProvisionProjectWorkflowsCreatesReferencesLikeThePublicPath() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithWorkflows("Provision Created", 2);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();
            ConnectedUser directConnectedUser = createConnectedUserWithConnection();

            List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
                .workflowTemplates();

            String firstWorkflowUuid = templates.getFirst()
                .workflowUuid();
            String secondWorkflowUuid = templates.getLast()
                .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(projectId),
                List.of(
                    new ProvisionConnectedUserProjectWorkflowDTO(firstWorkflowUuid, null),
                    new ProvisionConnectedUserProjectWorkflowDTO(secondWorkflowUuid, null)));

            ConnectedUserProjectWorkflow directReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                directConnectedUser.getExternalId(), firstWorkflowUuid, Environment.PRODUCTION);

            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
                .extracting(
                    ConnectedUserProjectWorkflow::getAutomationUuid, ConnectedUserProjectWorkflow::isEnabled,
                    ConnectedUserProjectWorkflow::isDangling,
                    reference -> reference.getProjectDeploymentId() != null)
                .containsExactlyInAnyOrder(
                    tuple(firstWorkflowUuid, true, false, directReference.getProjectDeploymentId() != null),
                    tuple(secondWorkflowUuid, true, false, directReference.getProjectDeploymentId() != null));
        }

        @Test
        void testProvisionProjectWorkflowsRejectsUnprovisionableUuidBeforeWriting() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithWorkflows("Provision Rejected", 2);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
                .workflowTemplates();

            String validWorkflowUuid = templates.getFirst()
                .workflowUuid();
            String turnedOffWorkflowUuid = templates.getLast()
                .workflowUuid();

            AutomationProjectConfiguration configuration = automationProjectConfigurationService
                .getConfigurations(projectId, Environment.PRODUCTION.ordinal())
                .stream()
                .findFirst()
                .orElseThrow();

            automationProjectConfigurationFacade.enableConfigurationWorkflow(
                configuration.getId(), turnedOffWorkflowUuid, false);

            assertThatThrownBy(() -> connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), configuration.getId(),
                List.of(
                    new ProvisionConnectedUserProjectWorkflowDTO(validWorkflowUuid, null),
                    new ProvisionConnectedUserProjectWorkflowDTO(turnedOffWorkflowUuid, null))))
                        .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                            configurationException.getErrorKey())
                                .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE
                                    .getErrorKey()));

            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
            assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
                .isEmpty();
        }

        @Test
        void testProvisionProjectWorkflowsKeepsEarlierReferencesWhenALaterWorkflowFails() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithWorkflows("Provision Partial", 2);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            List<ConnectedUserWorkflowTemplateDTO> templates = findPublishedProject(connectedUser, projectId)
                .workflowTemplates();

            String firstWorkflowUuid = templates.getFirst()
                .workflowUuid();
            String failingWorkflowUuid = templates.getLast()
                .workflowUuid();

            doThrow(new IllegalStateException("Trigger registration failed"))
                .when(connectedUserWorkflowReferenceFacade)
                .getOrCreateReference(
                    anyString(), eq(failingWorkflowUuid), any(Environment.class), anyMap(), any());

            long connectedUserId = connectedUser.getId();
            long configurationId = getConfigurationId(projectId);
            List<ProvisionConnectedUserProjectWorkflowDTO> workflows = List.of(
                new ProvisionConnectedUserProjectWorkflowDTO(firstWorkflowUuid, null),
                new ProvisionConnectedUserProjectWorkflowDTO(failingWorkflowUuid, null));

            assertThatThrownBy(() -> connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUserId, configurationId, workflows))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
                .extracting(ConnectedUserProjectWorkflow::getAutomationUuid,
                    ConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(firstWorkflowUuid, true));
        }

        @Test
        void testProvisionProjectWorkflowsKeepsReferenceWhenConnectionIsMissing() {
            givenSlackAutomationEnvironment();

            long needsConnectionProjectId =
                createProjectWithDefinition("Provision Needs Connection", SLACK_WORKFLOW_DEFINITION);
            long standaloneProjectId = createProjectWithDefinition(
                "Provision Standalone", NO_CONNECTION_WORKFLOW_DEFINITION);

            publishAndPinProject(needsConnectionProjectId);
            publishAndPinProject(standaloneProjectId);

            ConnectedUser connectedUser = connectedUserService.createConnectedUser(
                "provisionable-user-" + UUID.randomUUID(), Environment.PRODUCTION);

            String needsConnectionWorkflowUuid = findPublishedProject(connectedUser, needsConnectionProjectId)
                .workflowTemplates()
                .getFirst()
                .workflowUuid();
            String standaloneWorkflowUuid = findPublishedProject(connectedUser, standaloneProjectId)
                .workflowTemplates()
                .getFirst()
                .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(needsConnectionProjectId),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(needsConnectionWorkflowUuid, null)));
            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(standaloneProjectId),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(standaloneWorkflowUuid, null)));

            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
                .extracting(ConnectedUserProjectWorkflow::getAutomationUuid)
                .containsExactlyInAnyOrder(needsConnectionWorkflowUuid, standaloneWorkflowUuid);

            ConnectedUserProjectWorkflowDTO needsConnectionWorkflow = findProjectWorkflow(
                connectedUser, needsConnectionWorkflowUuid);

            assertThat(needsConnectionWorkflow.attentionReason())
                .isEqualTo(ConnectedUserReferenceAttentionResolver.MISSING_CONNECTION_PREFIX + "slack");
        }

        @Test
        void testGetProvisionableProjectsListsTheComponentsAWorkflowNeedsAConnectionFor() {
            givenSlackAutomationEnvironment();

            ComponentDefinition slackComponentDefinition = mock(ComponentDefinition.class);

            when(slackComponentDefinition.getName()).thenReturn("slack");
            when(slackComponentDefinition.getTitle()).thenReturn("Slack");
            when(slackComponentDefinition.isConnectionRequired()).thenReturn(true);
            when(componentDefinitionService.fetchComponentDefinition(eq("slack"), anyInt()))
                .thenReturn(Optional.of(slackComponentDefinition));

            long projectId = createProjectWithDefinition("Provisionable Components", SLACK_WORKFLOW_DEFINITION);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId);

            assertThat(provisionableProject.workflows())
                .singleElement()
                .satisfies(workflow -> assertThat(workflow.components())
                    .extracting(ConnectedUserWorkflowTemplateDTO.Component::name,
                        ConnectedUserWorkflowTemplateDTO.Component::title)
                    .containsExactly(tuple("slack", "Slack")));
        }

        @Test
        void testGetProvisionableProjectsOmitsTheConnectionsAndInputsTheConfigurationProvides() {
            givenSlackAutomationEnvironment();

            ComponentDefinition slackComponentDefinition = mock(ComponentDefinition.class);

            when(slackComponentDefinition.getName()).thenReturn("slack");
            when(slackComponentDefinition.getTitle()).thenReturn("Slack");
            when(slackComponentDefinition.isConnectionRequired()).thenReturn(true);
            when(componentDefinitionService.fetchComponentDefinition(eq("slack"), anyInt()))
                .thenReturn(Optional.of(slackComponentDefinition));

            long projectId = createProjectWithDefinition("Provisionable Provided", CHANNEL_INPUT_WORKFLOW_DEFINITION);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            long administratorConnectionId = NEXT_CONNECTION_ID.getAndIncrement();
            Connection administratorConnection = new Connection();

            administratorConnection.setComponentName("slack");
            administratorConnection.setEnvironmentId(Environment.PRODUCTION.ordinal());
            administratorConnection.setId(administratorConnectionId);
            administratorConnection.setType(PlatformType.EMBEDDED);

            when(connectionService.fetchConnection(administratorConnectionId))
                .thenReturn(Optional.of(administratorConnection));

            String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
                .getFirst()
                .workflowUuid();

            automationProjectConfigurationFacade.updateConfigurationWorkflow(
                getConfigurationId(projectId), workflowUuid, Map.of("slack", administratorConnectionId),
                Map.of("channel", "#provided"));

            ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId);

            assertThat(provisionableProject.workflows())
                .singleElement()
                .satisfies(workflow -> {
                    assertThat(workflow.components()).isEmpty();
                    assertThat(workflow.inputs()).isEmpty();
                });
        }

        @Test
        void testProvisionProjectWorkflowsWiresTheConnectionChosenForAComponent() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithDefinition("Provision Chosen Connection", SLACK_WORKFLOW_DEFINITION);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            long connectedUserId = Objects.requireNonNull(connectedUser.getId());
            long chosenConnectionId = connectedUserConnectionService.getConnectionIds(connectedUserId)
                .getFirst();
            long newerConnectionId = NEXT_CONNECTION_ID.getAndIncrement();

            registerEmbeddedConnection(newerConnectionId, Environment.PRODUCTION);

            connectedUserConnectionService.create(connectedUserId, newerConnectionId);

            String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
                .getFirst()
                .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUserId, getConfigurationId(projectId),
                List.of(
                    new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null,
                        Map.of("slack", chosenConnectionId))));

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade
                .getConnectedUserWorkflows(connectedUserId)
                .stream()
                .filter(
                    connectedUserProjectWorkflow -> workflowUuid
                        .equals(connectedUserProjectWorkflow.getAutomationUuid()))
                .findFirst()
                .orElseThrow();

            assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(
                Objects.requireNonNull(reference.getProjectDeploymentId())))
                    .flatExtracting(ProjectDeploymentWorkflow::getConnections)
                    .extracting(ProjectDeploymentWorkflowConnection::getConnectionId)
                    .containsExactly(chosenConnectionId);
        }

        @Test
        void testProvisionProjectWorkflowsPassesInputsThroughAndLeavesThemAloneWhenOmitted() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithDefinition("Provision Inputs", CHANNEL_INPUT_WORKFLOW_DEFINITION);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
                .getFirst()
                .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(projectId),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, Map.of("channel", "#alerts"))));

            assertThat(findProjectWorkflow(connectedUser, workflowUuid).inputValues())
                .isEqualTo(Map.of("channel", "#alerts"));

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(projectId),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

            assertThat(findProjectWorkflow(connectedUser, workflowUuid).inputValues())
                .isEqualTo(Map.of("channel", "#alerts"));
        }

        @Test
        void testGetProjectWorkflowReferenceValuesReturnsTheChosenConnectionAndInputs() {
            givenSlackAutomationEnvironment();

            long projectId = createProjectWithDefinition("Reference Values", CHANNEL_INPUT_WORKFLOW_DEFINITION);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            long connectedUserId = Objects.requireNonNull(connectedUser.getId());
            long chosenConnectionId = connectedUserConnectionService.getConnectionIds(connectedUserId)
                .getFirst();

            String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
                .getFirst()
                .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUserId, getConfigurationId(projectId),
                List.of(
                    new ProvisionConnectedUserProjectWorkflowDTO(
                        workflowUuid, Map.of("channel", "#alerts"), Map.of("slack", chosenConnectionId))));

            ConnectedUserWorkflowReferenceValuesDTO referenceValues =
                connectedUserProjectFacade.getProjectWorkflowReferenceValues(
                    getReferenceId(connectedUserId, workflowUuid));

            assertThat(referenceValues.connectionIds()).isEqualTo(Map.of("slack", chosenConnectionId));
            assertThat(referenceValues.inputs()).isEqualTo(Map.of("channel", "#alerts"));
        }

        @Test
        void testGetProjectWorkflowReferenceValuesLeavesOutTheConnectionsAndInputsTheConfigurationProvides() {
            givenSlackAutomationEnvironment();

            long projectId =
                createProjectWithDefinition("Reference Provided Values", CHANNEL_INPUT_WORKFLOW_DEFINITION);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            long connectedUserId = Objects.requireNonNull(connectedUser.getId());
            long administratorConnectionId = NEXT_CONNECTION_ID.getAndIncrement();
            Connection administratorConnection = new Connection();

            administratorConnection.setComponentName("slack");
            administratorConnection.setEnvironmentId(Environment.PRODUCTION.ordinal());
            administratorConnection.setId(administratorConnectionId);
            administratorConnection.setType(PlatformType.EMBEDDED);

            when(connectionService.fetchConnection(administratorConnectionId))
                .thenReturn(Optional.of(administratorConnection));

            String workflowUuid = findPublishedProject(connectedUser, projectId).workflowTemplates()
                .getFirst()
                .workflowUuid();

            automationProjectConfigurationFacade.updateConfigurationWorkflow(
                getConfigurationId(projectId), workflowUuid, Map.of("slack", administratorConnectionId),
                Map.of("channel", "#provided"));

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUserId, getConfigurationId(projectId),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

            ConnectedUserWorkflowReferenceValuesDTO referenceValues =
                connectedUserProjectFacade.getProjectWorkflowReferenceValues(
                    getReferenceId(connectedUserId, workflowUuid));

            assertThat(referenceValues.connectionIds()).isEmpty();
            assertThat(referenceValues.inputs()).isEmpty();
        }

        @Test
        void testGetProjectWorkflowReferenceValuesRejectsAWorkflowThatIsNotAReference() {
            assertThatThrownBy(() -> connectedUserProjectFacade.getProjectWorkflowReferenceValues(Long.MAX_VALUE))
                .isInstanceOf(ConfigurationException.class);
        }

        @Test
        void testProvisionAssignsTheChosenConfigurationIgnoringItsExpression() {
            givenSlackAutomationEnvironment();
            givenRealPermissionEvaluator();

            long projectId = createProjectWithWorkflows("Provision Beta", 1);

            publishAndPinProject(projectId);

            AutomationProjectConfiguration betaConfiguration = createBetaConfiguration(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            String workflowUuid = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                    .workflows()
                    .getFirst()
                    .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), betaConfiguration.getId(),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

            assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
                .map(ConnectedUserProjectConfiguration::getConfigurationId)
                .contains(betaConfiguration.getId());
            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
                .extracting(ConnectedUserProjectWorkflow::getAutomationUuid,
                    ConnectedUserProjectWorkflow::isEnabled)
                .containsExactly(tuple(workflowUuid, true));
            assertThat(connectedUserProjectFacade.getConnectedUserProjectConfigurations(connectedUser.getId()))
                .containsExactly(
                    new ConnectedUserAutomationProjectConfigurationDTO(
                        projectId, betaConfiguration.getId(), "Beta", betaConfiguration.getProjectVersion(), true,
                        true));
        }

        @Test
        void testProvisionWithDifferentConfigurationForAssignedUserThrows() {
            givenSlackAutomationEnvironment();
            givenRealPermissionEvaluator();

            long projectId = createProjectWithWorkflows("Provision Reassign", 1);

            publishAndPinProject(projectId);

            long alphaConfigurationId = getConfigurationId(projectId);

            AutomationProjectConfiguration betaConfiguration = createBetaConfiguration(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            String workflowUuid = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                    .workflows()
                    .getFirst()
                    .workflowUuid();

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), alphaConfigurationId,
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

            assertThatThrownBy(() -> connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), betaConfiguration.getId(),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                                .getErrorKey()));

            assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
                .map(ConnectedUserProjectConfiguration::getConfigurationId)
                .contains(alphaConfigurationId);

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), alphaConfigurationId,
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null)));

            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId()))
                .extracting(ConnectedUserProjectWorkflow::getAutomationUuid)
                .containsExactly(workflowUuid);
        }

        @Test
        void testProvisionableProjectsListEveryEnabledConfigurationAndMarkSelfServe() {
            givenSlackAutomationEnvironment();
            givenRealPermissionEvaluator();

            long projectId = createProjectWithWorkflows("Provisionable Configurations", 2);

            publishAndPinProject(projectId);

            long alphaConfigurationId = getConfigurationId(projectId);

            AutomationProjectConfiguration betaConfiguration = createBetaConfiguration(projectId);

            automationProjectConfigurationService.create(
                projectId, Environment.PRODUCTION.ordinal(), "Disabled", null, betaConfiguration.getProjectVersion(),
                false, true);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            List<String> workflowUuids = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                    .workflows()
                    .stream()
                    .map(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
                    .toList();

            String betaTurnedOffWorkflowUuid = workflowUuids.getFirst();
            String betaReleasedWorkflowUuid = workflowUuids.getLast();

            automationProjectConfigurationFacade.enableConfigurationWorkflow(
                betaConfiguration.getId(), betaTurnedOffWorkflowUuid, false);

            ProvisionableAutomationProjectDTO provisionableProject = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId);

            assertThat(provisionableProject.assignedConfigurationId()).isNull();
            assertThat(provisionableProject.configurations())
                .extracting(
                    ProvisionableAutomationProjectDTO.Configuration::id,
                    ProvisionableAutomationProjectDTO.Configuration::selfServe)
                .containsExactly(tuple(alphaConfigurationId, true), tuple(betaConfiguration.getId(), false));
            assertThat(provisionableProject.configurations()
                .getLast()
                .workflows())
                    .extracting(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
                    .containsExactly(betaReleasedWorkflowUuid);
            assertThat(provisionableProject.workflows())
                .extracting(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
                .containsExactlyInAnyOrderElementsOf(workflowUuids);

            ConnectedUser betaConnectedUser = connectedUserService.createConnectedUser(
                "beta-user-" + UUID.randomUUID(), Environment.PRODUCTION, null, null, Map.of("beta", "true"));

            assertThat(
                findProvisionableProject(
                    connectedUserProjectFacade.getProvisionableProjects(betaConnectedUser.getId()), projectId)
                        .configurations())
                            .extracting(
                                ProvisionableAutomationProjectDTO.Configuration::id,
                                ProvisionableAutomationProjectDTO.Configuration::selfServe)
                            .containsExactly(tuple(alphaConfigurationId, false),
                                tuple(betaConfiguration.getId(), true));

            connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), betaConfiguration.getId(),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(betaReleasedWorkflowUuid, null)));

            ProvisionableAutomationProjectDTO assignedProvisionableProject = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId);

            assertThat(assignedProvisionableProject.assignedConfigurationId()).isEqualTo(betaConfiguration.getId());
            assertThat(assignedProvisionableProject.workflows())
                .extracting(
                    ProvisionableAutomationProjectDTO.Workflow::workflowUuid,
                    ProvisionableAutomationProjectDTO.Workflow::provisioned)
                .containsExactly(tuple(betaReleasedWorkflowUuid, true));
        }

        @Test
        void testProvisionOntoDisabledConfigurationThrowsAndAssignsNothing() {
            givenSlackAutomationEnvironment();
            givenRealPermissionEvaluator();

            long projectId = createProjectWithWorkflows("Provision Disabled", 1);

            publishAndPinProject(projectId);

            AutomationProjectConfiguration alphaConfiguration = automationProjectConfigurationService
                .getConfiguration(getConfigurationId(projectId));

            AutomationProjectConfiguration disabledConfiguration = automationProjectConfigurationService
                .create(
                    projectId, Environment.PRODUCTION.ordinal(), "Disabled", null,
                    alphaConfiguration.getProjectVersion(),
                    false, true);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            String workflowUuid = findProvisionableProject(
                connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()), projectId)
                    .workflows()
                    .getFirst()
                    .workflowUuid();

            assertThatThrownBy(() -> connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), disabledConfiguration.getId(),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                                .getErrorKey()));

            assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
                .isEmpty();
            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
        }

        @Test
        void testProvisionIntoProjectWhoseExpressionFailsThrowsAndAssignsNothing() {
            givenSlackAutomationEnvironment();
            givenRealPermissionEvaluator();

            long projectId = automationProjectFacade.createProject(
                "Provision Gated " + UUID.randomUUID(), "", null, List.of(), "metadata['beta'] == 'true'");

            String workflowUuid = automationProjectFacade.createProjectWorkflow(
                projectId, SLACK_WORKFLOW_DEFINITION, null);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            assertThat(connectedUserProjectFacade.getProvisionableProjects(connectedUser.getId()))
                .extracting(ProvisionableAutomationProjectDTO::projectId)
                .doesNotContain(projectId);

            assertThatThrownBy(() -> connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(projectId),
                List.of(new ProvisionConnectedUserProjectWorkflowDTO(workflowUuid, null))))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH
                                .getErrorKey()));

            assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
                .isEmpty();
            assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
        }

        @Test
        void testProvisionWithNoWorkflowsThrows() {
            givenSlackAutomationEnvironment();
            givenRealPermissionEvaluator();

            long projectId = createProjectWithWorkflows("Provision Nothing", 1);

            publishAndPinProject(projectId);

            ConnectedUser connectedUser = createConnectedUserWithConnection();

            assertThatThrownBy(() -> connectedUserProjectFacade.provisionProjectWorkflows(
                connectedUser.getId(), getConfigurationId(projectId), List.of()))
                    .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                        configurationException.getErrorKey())
                            .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE.getErrorKey()));

            assertThat(connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId))
                .isEmpty();
        }

        private AutomationProjectConfiguration createBetaConfiguration(long projectId) {
            AutomationProjectConfiguration alphaConfiguration = automationProjectConfigurationService
                .getConfiguration(getConfigurationId(projectId));

            return automationProjectConfigurationService.create(
                projectId, Environment.PRODUCTION.ordinal(), "Beta", "metadata['beta'] == 'true'",
                alphaConfiguration.getProjectVersion(), true, true);
        }

        private void givenRealPermissionEvaluator() {
            EmbeddedPermissionEvaluator realEmbeddedPermissionEvaluator = new EmbeddedPermissionEvaluator(
                SpelEvaluator.create());

            when(embeddedPermissionEvaluator.evaluate(any(), any()))
                .thenAnswer(invocation -> realEmbeddedPermissionEvaluator.evaluate(
                    invocation.getArgument(0), invocation.getArgument(1)));
        }

        private long getConfigurationId(long projectId) {
            AutomationProjectConfiguration configuration = automationProjectConfigurationService
                .getConfigurations(projectId, Environment.PRODUCTION.ordinal())
                .getFirst();

            return Objects.requireNonNull(configuration.getId());
        }

        private ConnectedUserProjectWorkflowDTO findProjectWorkflow(ConnectedUser connectedUser, String workflowUuid) {
            return connectedUserProjectFacade.getConnectedUserProjects(connectedUser.getId(), Environment.PRODUCTION)
                .stream()
                .map(ConnectedUserProjectDTO::connectedUserProjectWorkflows)
                .flatMap(List::stream)
                .filter(workflow -> workflowUuid.equals(workflow.automationUuid()))
                .findFirst()
                .orElseThrow();
        }

        private long createProjectWithDefinition(String name, String definition) {
            long projectId = automationProjectFacade.createProject(
                name + " " + UUID.randomUUID(), "", null, List.of(), null);

            automationProjectFacade.createProjectWorkflow(projectId, definition, null);

            return projectId;
        }

        private long createProjectWithWorkflows(String name, int workflowCount) {
            long projectId = automationProjectFacade.createProject(
                name + " " + UUID.randomUUID(), "", null, List.of(), null);

            for (int index = 0; index < workflowCount; index++) {
                automationProjectFacade.createProjectWorkflow(projectId, SLACK_WORKFLOW_DEFINITION, null);
            }

            return projectId;
        }

        private AutomationProjectDTO findPublishedProject(ConnectedUser connectedUser, long projectId) {
            return automationProjectFacade
                .getPublishedProjects(connectedUser.getExternalId(), Environment.PRODUCTION)
                .stream()
                .filter(project -> project.id() == projectId)
                .findFirst()
                .orElseThrow();
        }

        private ProvisionableAutomationProjectDTO findProvisionableProject(
            List<ProvisionableAutomationProjectDTO> provisionableProjects, long projectId) {

            return provisionableProjects.stream()
                .filter(project -> project.projectId() == projectId)
                .findFirst()
                .orElseThrow();
        }

        private ConnectedUser createConnectedUserWithConnection() {
            ConnectedUser connectedUser = connectedUserService.createConnectedUser(
                "provisionable-user-" + UUID.randomUUID(), Environment.PRODUCTION);

            long connectionId = NEXT_CONNECTION_ID.getAndIncrement();

            registerEmbeddedConnection(connectionId, Environment.PRODUCTION);

            connectedUserConnectionService.create(Objects.requireNonNull(connectedUser.getId()), connectionId);

            return connectedUser;
        }

        private void registerEmbeddedConnection(long connectionId, Environment environment) {
            Connection connection = new Connection();

            connection.setEnvironmentId(environment.ordinal());
            connection.setId(connectionId);
            connection.setType(PlatformType.EMBEDDED);

            embeddedConnections.put(connectionId, connection);
        }

        private void givenSlackAutomationEnvironment() {
            ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

            when(componentDefinitionService.getComponentDefinition(anyString(), anyInt()))
                .thenReturn(new ComponentDefinition("slack"));
            when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
                .thenReturn(List.of(slot));
            when(componentConnectionFacade.getComponentConnection(anyString(), eq("postMessage1"), eq("slack")))
                .thenReturn(slot);
            when(connectionFacade.getConnections(anyList(), eq(PlatformType.EMBEDDED)))
                .thenAnswer(invocation -> {
                    List<Long> connectionIds = invocation.getArgument(0);

                    return connectionIds.stream()
                        .map(connectionId -> ConnectionDTO.builder()
                            .componentName("slack")
                            .id(connectionId)
                            .build())
                        .toList();
                });
            when(connectionService.getConnections(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> connectionIds = invocation.getArgument(0);

                    return connectionIds.stream()
                        .map(embeddedConnections::get)
                        .filter(Objects::nonNull)
                        .toList();
                });
            when(connectionService.getConnection(anyLong()))
                .thenAnswer(invocation -> {
                    Connection connection = new Connection();

                    connection.setComponentName("slack");
                    connection.setId(invocation.getArgument(0));

                    return connection;
                });
            when(embeddedPermissionEvaluator.evaluate(any(), any()))
                .thenReturn(true);
            when(principalJobService.getJobIds(
                any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyInt()))
                    .thenReturn(Page.empty());
        }

        private void markDangling(ConnectedUserProjectWorkflow reference) {
            ConnectedUserProjectWorkflow storedReference =
                connectedUserProjectWorkflowRepository.findById(reference.getId())
                    .orElseThrow();

            storedReference.setDangling(true);
            storedReference.setDanglingReason("Removed from the automation project on redeploy");
            storedReference.setEnabled(false);

            connectedUserProjectWorkflowRepository.save(storedReference);
        }

        private void publishAndPinProject(long projectId) {
            AutomationProjectConfigurationTestSupport.publishAndPinProject(
                automationProjectFacade, automationProjectConfigurationService, projectId);
        }

        private long getReferenceId(long connectedUserId, String workflowUuid) {
            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade
                .getConnectedUserWorkflows(connectedUserId)
                .stream()
                .filter(
                    connectedUserProjectWorkflow -> workflowUuid
                        .equals(connectedUserProjectWorkflow.getAutomationUuid()))
                .findFirst()
                .orElseThrow();

            return Objects.requireNonNull(reference.getId());
        }
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    private void publishAndPinProject(long projectId) {
        AutomationProjectConfigurationTestSupport.publishAndPinProject(
            automationProjectFacade, automationProjectConfigurationService, projectId);
    }
}

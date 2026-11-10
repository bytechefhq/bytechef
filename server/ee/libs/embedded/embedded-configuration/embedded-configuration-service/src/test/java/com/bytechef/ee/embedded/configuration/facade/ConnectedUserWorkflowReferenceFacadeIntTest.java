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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationTemplateNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
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
import java.lang.reflect.Method;
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
import java.util.concurrent.TimeoutException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
        ConnectedUserWorkflowReferenceFacadeIntTest.ConnectedUserServiceConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user,execution",
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
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserWorkflowReferenceFacadeIntTest {

    private static final AtomicLong NEXT_CONNECTION_ID = new AtomicLong(1000L);

    private static final String HIDDEN_PERMISSION_EXPRESSION = "connectedUser.metadata.plan == 'hidden'";

    private static final String SLACK_WORKFLOW_DEFINITION = """
        {"label":"Post","triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    private static final String SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
        {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
         "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
        """;

    @Autowired
    private AutomationProjectConfigurationService automationProjectConfigurationService;

    @MockitoSpyBean
    private AutomationProjectFacade automationProjectFacade;

    @MockitoBean
    private AutomationProjectConfigurationUpdatedEventListener automationProjectConfigurationUpdatedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectUserProjectRepository connectUserProjectRepository;

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

    @Autowired
    private ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver;

    @Autowired
    private ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;

    @Autowired
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

    @MockitoSpyBean
    private ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;

    @MockitoSpyBean
    private ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;

    @MockitoSpyBean
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    private final Map<Long, Connection> embeddedConnections = new ConcurrentHashMap<>();

    @Autowired
    private EmbeddedPermissionEvaluator embeddedPermissionEvaluator;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

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

    @BeforeEach
    void setUp() {
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

        ComponentConnection slot = new ComponentConnection("slack", 1, "postMessage1", "slack", true);

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
    }

    @Test
    void testTwoUsersReferencingTheSameAutomationGetIndependentWiring() {
        long automationProjectId = createAutomationProject("Two Users");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser userA = createConnectedUser(Environment.PRODUCTION);
        ConnectedUser userB = createConnectedUser(Environment.PRODUCTION);

        long userAConnectionId = givenOwnedConnection(userA);
        long userBConnectionId = givenOwnedConnection(userB);

        ConnectedUserProjectWorkflow userAReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            userA.getExternalId(), workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow userBReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            userB.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(userAReference.getProjectDeploymentId()).isNotEqualTo(userBReference.getProjectDeploymentId());

        assertThat(getDeployment(userAReference).getProjectVersion()).isEqualTo(1);
        assertThat(fetchRow(userAReference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections())
                    .containsExactly(
                        new ProjectDeploymentWorkflowConnection(userAConnectionId, "slack", "postMessage1"));
            });

        assertThat(getDeployment(userBReference).getProjectVersion()).isEqualTo(1);
        assertThat(fetchRow(userBReference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections())
                    .containsExactly(
                        new ProjectDeploymentWorkflowConnection(userBConnectionId, "slack", "postMessage1"));
            });
    }

    @Test
    void testGetOrCreateReferenceProvisionsDistinctDeploymentsPerEnvironment() {
        long automationProjectId = createAutomationProject("Per Environment");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        automationProjectConfigurationService.create(automationProjectId,
            Environment.DEVELOPMENT.ordinal(), "Default", null, 1, true, true);

        String externalUserId = newExternalUserId();

        givenOwnedConnection(connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION));
        givenOwnedConnection(connectedUserService.createConnectedUser(externalUserId, Environment.DEVELOPMENT));

        ConnectedUserProjectWorkflow productionReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.PRODUCTION);
        ConnectedUserProjectWorkflow developmentReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            externalUserId, workflowUuid, Environment.DEVELOPMENT);

        assertThat(productionReference.getProjectDeploymentId())
            .isNotEqualTo(developmentReference.getProjectDeploymentId());
        assertThat(getDeployment(productionReference).getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(getDeployment(developmentReference).getEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testGetOrCreateReferenceReturnsExistingRowWithoutReprovisioning() {
        long automationProjectId = createAutomationProject("Existing Reference");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow existingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        clearInvocations(automationProjectFacade, connectedUserReferenceDeploymentManager,
            connectedUserService);

        ConnectedUserProjectWorkflow result = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(result.getId()).isEqualTo(existingReference.getId());
        assertThat(result.getProjectDeploymentId()).isEqualTo(existingReference.getProjectDeploymentId());
        assertThat(getReference(existingReference.getId()).getVersion()).isEqualTo(existingReference.getVersion());

        verifyNoInteractions(automationProjectFacade, connectedUserService);

        verify(connectedUserReferenceDeploymentManager).getDeployment(existingReference.getProjectDeploymentId());
        verify(connectedUserReferenceDeploymentManager).fetchConnectedUserId(
            existingReference.getProjectDeploymentId());
        verify(connectedUserReferenceDeploymentManager).fetchReleasedVersion(
            connectedUser.getId(), automationProjectId);
        verifyNoMoreInteractions(connectedUserReferenceDeploymentManager);
    }

    @Test
    void testGetOrCreateReferenceReResolvesAnExistingReferenceWithRequestedConnections() {
        long automationProjectId = createAutomationProject("Requested Connections");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        long firstConnectionId = givenOwnedConnection(connectedUser);
        long secondConnectionId = givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        long wiredConnectionId = getConnectionIds(reference).getFirst();
        long requestedConnectionId = wiredConnectionId == firstConnectionId ? secondConnectionId : firstConnectionId;

        clearInvocations(automationProjectFacade);

        ConnectedUserProjectWorkflow updatedReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION,
            Map.of("slack", requestedConnectionId));

        assertThat(updatedReference.isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections()).containsExactly(
                    new ProjectDeploymentWorkflowConnection(requestedConnectionId, "slack", "postMessage1"));
            });

        verifyNoInteractions(automationProjectFacade);
    }

    @Test
    void testMissingConnectionStillCreatesDisabledReferenceAndRethrows() {
        long automationProjectId = createAutomationProject("Missing Connection");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class)
                .extracting("componentName")
                .isEqualTo("slack");

        ConnectedUserProjectWorkflow savedReference = getOnlyReference(connectedUser);

        assertThat(savedReference.isEnabled()).isFalse();
        assertThat(savedReference.getAutomationUuid()).isEqualTo(workflowUuid);
        assertThat(savedReference.getProjectWorkflowId()).isNull();
        assertThat(fetchRow(savedReference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getConnections()).isEmpty();
            });
    }

    @Test
    void testGetOrCreateReferenceLeavesReferenceDisabledWithoutErrorWhenARequiredInputHasNoValue() {
        long automationProjectId = createAutomationProject("Missing Input Provisioning");

        String workflowUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        long connectionId = givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isFalse();
        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getConnections())
                    .containsExactly(new ProjectDeploymentWorkflowConnection(connectionId, "slack", "postMessage1"));
            });
    }

    @Test
    void testEnableReferenceTogglesEnabledFlagAndProjectDeploymentWorkflow() {
        long automationProjectId = createAutomationProject("Enable Toggle");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        long connectionId = givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, false, Environment.PRODUCTION);

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::isEnabled)
            .isEqualTo(false);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION);

        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections())
                    .containsExactly(new ProjectDeploymentWorkflowConnection(connectionId, "slack", "postMessage1"));
            });
    }

    @Test
    void testHiddenProjectStaysProvisionableByWorkflowUuid() {
        long automationProjectId = createAutomationProject("Hidden From Hub");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.update(configuration.getId(), configuration.getName(),
            configuration.getPermissionExpression(), configuration.getProjectVersion(), true, false);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        List<AutomationProjectDTO> publishedProjects = automationProjectFacade.getPublishedProjects(
            connectedUser.getExternalId(), Environment.PRODUCTION);

        AutomationProjectDTO hiddenProject = publishedProjects.stream()
            .filter(project -> project.id() == automationProjectId)
            .findFirst()
            .orElseThrow();

        assertThat(hiddenProject.automationHubVisible()).isFalse();
        assertThat(hiddenProject.workflowTemplates())
            .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
            .contains(workflowUuid);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.getId()).isNotNull();
        assertThat(getReference(reference.getId()).isDangling()).isFalse();
    }

    @Test
    void testEnableReferenceWhileAdministrativelyOffSavesTheIntentButKeepsTheRowOff() {
        long automationProjectId = createAutomationProject("Enable Administratively Off");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, false, Environment.PRODUCTION);

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateEnabled(configuration.getId(), false);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION);

        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::isEnabled)
            .isEqualTo(false);
    }

    @Test
    void testEnableReferenceRewiresConnectionsWhenPreviouslyMissingConnectionWasFixed() {
        long automationProjectId = createAutomationProject("Rewire");

        String workflowUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            connectedUser.getExternalId(), workflowUuid, Map.of("channel", "general"), Environment.PRODUCTION);

        long connectionId = givenOwnedConnection(connectedUser);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = getOnlyReference(connectedUser);

        assertThat(reference.isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections())
                    .containsExactly(new ProjectDeploymentWorkflowConnection(connectionId, "slack", "postMessage1"));
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "general"));
            });
    }

    @Test
    void testEnableReferenceStillThrowsWhenConnectionIsStillMissing() {
        long automationProjectId = createAutomationProject("Still Missing");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class)
                .extracting("componentName")
                .isEqualTo("slack");

        ConnectedUserProjectWorkflow reference = getOnlyReference(connectedUser);

        assertThat(reference.isEnabled()).isFalse();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getConnections()).isEmpty();
            });
    }

    @Test
    void testEnableReferenceThrowsMissingInputWhenARequiredInputHasNoValue() {
        long automationProjectId = createAutomationProject("Missing Input Enable");

        String workflowUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        long connectionId = givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingInputException.class)
                .extracting("inputName")
                .isEqualTo("channel");

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isFalse();
                assertThat(row.getConnections())
                    .containsExactly(new ProjectDeploymentWorkflowConnection(connectionId, "slack", "postMessage1"));
            });
    }

    @Test
    void testEnableReferenceRefusesToEnableADanglingReference() {
        long automationProjectId = createAutomationProject("Enable Dangling");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        markDangling(reference);

        clearInvocations(connectedUserReferenceDeploymentManager, connectedUserReferenceRolloutManager);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class);

        verifyNoInteractions(connectedUserReferenceDeploymentManager, connectedUserReferenceRolloutManager);

        assertThat(getReference(reference.getId())).satisfies(danglingReference -> {
            assertThat(danglingReference.isDangling()).isTrue();
            assertThat(danglingReference.isEnabled()).isFalse();
        });
    }

    @Test
    void testEnableReferenceRefusedBecauseTheCatchUpLeftItDanglingKeepsTheCatchUp() throws NoSuchMethodException {
        long automationProjectId = createAutomationProject("Catch Up Dangling");

        addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, false, Environment.PRODUCTION);

        automationProjectFacade.deleteProjectWorkflow(workflowUuid);
        publishAndPinProject(automationProjectId);

        clearInvocations(connectedUserReferenceDeploymentManager);

        Throwable throwable = catchThrowable(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION));

        Method enableReferenceMethod = ConnectedUserWorkflowReferenceFacadeImpl.class.getMethod(
            "enableReference", String.class, String.class, boolean.class, Environment.class);
        Transactional transactional = enableReferenceMethod.getAnnotation(Transactional.class);

        assertThat(throwable)
            .isInstanceOf(DanglingReferenceException.class)
            .isInstanceOf(ConfigurationException.class);
        assertThat(transactional.noRollbackFor()).contains(DanglingReferenceException.class);

        verify(connectedUserReferenceDeploymentManager, never()).putWorkflows(anyLong(), anyInt(), anyMap());

        assertThat(getReference(reference.getId())).satisfies(danglingReference -> {
            assertThat(danglingReference.isDangling()).isTrue();
            assertThat(danglingReference.isEnabled()).isFalse();
        });
        assertThat(projectDeploymentService.fetchProjectDeployment(reference.getProjectDeploymentId())).isEmpty();
    }

    @Test
    void testDisableReferenceSkipsTheCatchUpAndDisablesTheRowAtTheCurrentVersion() {
        long automationProjectId = createAutomationProject("Disable Behind");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        publishAndPinProject(automationProjectId);

        doThrow(new IllegalStateException("Trigger registration failed"))
            .when(connectedUserReferenceRolloutManager)
            .rollOutDeploymentIfBehind(anyLong());

        clearInvocations(connectedUserReferenceRolloutManager);

        assertThatCode(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, false, Environment.PRODUCTION))
                .doesNotThrowAnyException();

        verify(connectedUserReferenceRolloutManager, never()).rollOutDeploymentIfBehind(anyLong());

        assertThat(getReference(reference.getId()).isEnabled()).isFalse();
        assertThat(getDeployment(reference).getProjectVersion()).isEqualTo(1);
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.getWorkflowId()).isEqualTo(getWorkflowId(automationProjectId, 1, workflowUuid));
                assertThat(row.isEnabled()).isFalse();
            });
    }

    @Test
    void testDisableDanglingReferenceRemovesItsRow() {
        long automationProjectId = createAutomationProject("Disable Dangling");

        String siblingWorkflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), siblingWorkflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        markDangling(reference);

        assertThat(fetchRow(reference)).isPresent();

        clearInvocations(connectedUserReferenceDeploymentManager, connectedUserReferenceRolloutManager);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, false, Environment.PRODUCTION);

        verify(connectedUserReferenceDeploymentManager).removeWorkflow(reference.getProjectDeploymentId(),
            workflowUuid);
        verify(connectedUserReferenceDeploymentManager, never()).putWorkflows(anyLong(), anyInt(), anyMap());
        verifyNoInteractions(connectedUserReferenceRolloutManager);

        assertThat(getReference(reference.getId())).satisfies(danglingReference -> {
            assertThat(danglingReference.isDangling()).isTrue();
            assertThat(danglingReference.isEnabled()).isFalse();
        });
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactly(getWorkflowId(automationProjectId, 1, siblingWorkflowUuid));
    }

    @Test
    void testEnableReferenceThrowsWhenNoReferenceExists() {
        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        String unknownWorkflowUuid = UUID.randomUUID()
            .toString();

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), unknownWorkflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testDeleteReferenceRemovesItsDeploymentRowAndTheReferenceRow() {
        long automationProjectId = createAutomationProject("Delete Reference");

        String siblingWorkflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), siblingWorkflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.deleteReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactly(getWorkflowId(automationProjectId, 1, siblingWorkflowUuid));
    }

    @Test
    void testDeleteDanglingReferenceAlsoRemovesItsRow() {
        long automationProjectId = createAutomationProject("Delete Dangling");

        String siblingWorkflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), siblingWorkflowUuid, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        markDangling(reference);

        assertThat(fetchRow(reference)).isPresent();

        connectedUserWorkflowReferenceFacade.deleteReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
        assertThat(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(reference.getProjectDeploymentId()))
            .extracting(ProjectDeploymentWorkflow::getWorkflowId)
            .containsExactly(getWorkflowId(automationProjectId, 1, siblingWorkflowUuid));
    }

    @Test
    void testUpdateReferenceInputsWritesTheRowUnderTheConnectedUsersLock() throws Exception {
        long automationProjectId = createAutomationProject("Inputs Lock");

        String workflowUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        runWhileTheConnectedUsersProjectIsLocked(
            reference.getConnectedUserProjectId(),
            () -> connectedUserWorkflowReferenceFacade.updateReferenceInputs(
                connectedUser.getExternalId(), workflowUuid, Map.of("channel", "#alerts"), Environment.PRODUCTION),
            () -> assertThat(fetchRow(reference))
                .get()
                .satisfies(row -> assertThat(row.getInputs()).isNullOrEmpty()));

        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::getInputs)
            .isEqualTo(Map.of("channel", "#alerts"));
    }

    @Test
    void testFillingTheMissingInputResumesTheWorkflow() {
        long automationProjectId = createAutomationProject("Resume On Input");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOutAndApply(
            automationProjectId, Environment.PRODUCTION.ordinal());

        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::isEnabled)
            .isEqualTo(false);
        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getReference(reference.getId()))
            .attentionReason()).isEqualTo("INPUT_REQUIRED:channel");

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            connectedUser.getExternalId(), workflowUuid, Map.of("channel", "#alerts"), Environment.PRODUCTION);

        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#alerts"));
            });
        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
        assertThat(connectedUserReferenceAttentionResolver.resolve(getReference(reference.getId()))
            .attentionReason()).isNull();
    }

    @Test
    void testEnablingAPausedWorkflowKeepsTheSwitchOn() {
        long automationProjectId = createAutomationProject("Enable Paused");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        String draftWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);
        Workflow draftWorkflow = workflowService.getWorkflow(draftWorkflowId);

        workflowService.update(
            draftWorkflowId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION, draftWorkflow.getVersion());

        publishAndPinProject(automationProjectId);

        connectedUserReferenceRolloutManager.rollOutAndApply(
            automationProjectId, Environment.PRODUCTION.ordinal());

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION))
                .isInstanceOf(MissingInputException.class)
                .extracting("inputName")
                .isEqualTo("channel");

        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::isEnabled)
            .isEqualTo(false);

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            connectedUser.getExternalId(), workflowUuid, Map.of("channel", "#alerts"), Environment.PRODUCTION);

        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::isEnabled)
            .isEqualTo(true);
        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
    }

    @Test
    void testUpdateReferenceInputsRefusesADanglingReference() {
        long automationProjectId = createAutomationProject("Inputs Dangling");

        String workflowUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            connectedUser.getExternalId(), workflowUuid, Map.of("channel", "#before"), Environment.PRODUCTION);

        markDangling(reference);

        clearInvocations(connectedUserReferenceDeploymentManager);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.updateReferenceInputs(
            connectedUser.getExternalId(), workflowUuid, Map.of("channel", "#alerts"), Environment.PRODUCTION))
                .isInstanceOf(DanglingReferenceException.class)
                .hasMessage("Reference to automation workflow " + workflowUuid + " is dangling");

        verify(connectedUserReferenceDeploymentManager, never()).updateInputs(anyLong(), anyString(), anyMap());

        assertThat(fetchRow(reference))
            .get()
            .extracting(ProjectDeploymentWorkflow::getInputs)
            .isEqualTo(Map.of("channel", "#before"));
    }

    @Test
    void testReferenceWritesLockTheConnectedUsersProjectRow() throws Exception {
        long automationProjectId = createAutomationProject("Delete Lock");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        runWhileTheConnectedUsersProjectIsLocked(
            reference.getConnectedUserProjectId(),
            () -> connectedUserWorkflowReferenceFacade.deleteReference(
                connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION),
            () -> assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isPresent());

        assertThat(connectedUserProjectWorkflowRepository.findById(reference.getId())).isEmpty();
    }

    @Test
    void testGetOrCreateReferenceProvisionsATemplateTheConnectedUserIsPermittedToSee() {
        long automationProjectId = createAutomationProject("Visible Template");

        String visibleWorkflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        addHiddenAutomation(automationProjectId);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), visibleWorkflowUuid, Environment.PRODUCTION);

        assertThat(reference.getAutomationUuid()).isEqualTo(visibleWorkflowUuid);
        assertThat(reference.isEnabled()).isTrue();
    }

    @Test
    void testGetOrCreateReferenceRejectsATemplateHiddenByThePermissionExpression() {
        long automationProjectId = createAutomationProject("Hidden Template");

        addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        String hiddenWorkflowUuid = addHiddenAutomation(automationProjectId);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        clearInvocations(connectedUserReferenceDeploymentManager);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), hiddenWorkflowUuid, Environment.PRODUCTION))
                .isInstanceOf(AutomationTemplateNotVisibleException.class);

        verify(connectedUserReferenceDeploymentManager, never()).getOrCreateDeployment(anyLong(), anyString(), any());
        verify(connectedUserReferenceDeploymentManager, never()).putWorkflows(anyLong(), anyInt(), anyMap());

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testProvisionReferenceThrowsWhenTheProjectIsNotReleasedInTheUsersEnvironment() {
        long automationProjectId = createAutomationProject("Not Released");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.DEVELOPMENT);

        givenOwnedConnection(connectedUser);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.DEVELOPMENT))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED
                            .getErrorKey()));

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testProvisionReferenceFailsWithoutQualifyingConfiguration() {
        EmbeddedPermissionEvaluator realEmbeddedPermissionEvaluator = new EmbeddedPermissionEvaluator(
            SpelEvaluator.create());

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenAnswer(invocation -> realEmbeddedPermissionEvaluator.evaluate(
                invocation.getArgument(0), invocation.getArgument(1)));

        long automationProjectId = createAutomationProject("Beta Only");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        automationProjectConfigurationService.create(
            automationProjectId, Environment.PRODUCTION.ordinal(), "Beta", "metadata['beta'] == 'true'", 1,
            true, true);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED
                            .getErrorKey()));

        assertThat(connectedUserProjectConfigurationService.fetchAssignment(
            connectedUser.getId(), automationProjectId)).isEmpty();
        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testProvisionReferenceThrowsWhenTemplateIsNotInThePinnedVersion() {
        long automationProjectId = createAutomationProject("Template Not Pinned");

        addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        String newerWorkflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        automationProjectFacade.publishProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), newerWorkflowUuid, Environment.PRODUCTION))
                .isInstanceOfSatisfying(ConfigurationException.class, configurationException -> assertThat(
                    configurationException.getErrorKey())
                        .isEqualTo(ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED
                            .getErrorKey()));

        assertThat(connectedUserReferenceDeploymentService.fetchConnectedUserReferenceDeployment(
            connectedUser.getId(), automationProjectId)).isEmpty();
    }

    @Test
    void testProvisionReferencePinsTheDeploymentToTheConfiguredVersion() {
        long automationProjectId = createAutomationProject("Pinned Deployment");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        automationProjectFacade.publishProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(getDeployment(reference).getProjectVersion()).isEqualTo(1);
    }

    @Test
    void testHiddenTemplateRejectionIsIndistinguishableFromAnUnknownUuid() {
        long automationProjectId = createAutomationProject("Indistinguishable");

        addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        String hiddenWorkflowUuid = addHiddenAutomation(automationProjectId);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        String unknownWorkflowUuid = UUID.randomUUID()
            .toString();

        Throwable hiddenThrowable = catchThrowable(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), hiddenWorkflowUuid, Environment.PRODUCTION));
        Throwable unknownThrowable = catchThrowable(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), unknownWorkflowUuid, Environment.PRODUCTION));

        assertThat(hiddenThrowable).isExactlyInstanceOf(AutomationTemplateNotVisibleException.class);
        assertThat(unknownThrowable).isExactlyInstanceOf(AutomationTemplateNotVisibleException.class);
        assertThat(hiddenThrowable).hasMessage("Not a published automation workflow template: " + hiddenWorkflowUuid);
        assertThat(unknownThrowable).hasMessage("Not a published automation workflow template: " + unknownWorkflowUuid);
    }

    @Test
    void testAlreadyProvisionedReferenceIsReturnedWithoutConsultingTheAutomations() {
        long automationProjectId = createAutomationProject("Narrowed Template");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow existingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(false);

        clearInvocations(automationProjectFacade);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.getId()).isEqualTo(existingReference.getId());
        assertThat(reference.isEnabled()).isTrue();

        verifyNoInteractions(automationProjectFacade);

        assertThat(automationProjectFacade.getPublishedProjects(
            connectedUser.getExternalId(), Environment.PRODUCTION))
                .flatMap(AutomationProjectDTO::workflowTemplates)
                .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .doesNotContain(workflowUuid);
    }

    @Test
    void testEnableReferenceUsesAConnectionTheConfigurationProvides() {
        long automationProjectId = createAutomationProject("Provided Connection Enable");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class);

        long administratorConnectionId = givenAdministratorConnection();

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", administratorConnectionId), Map.of());

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION);

        ConnectedUserProjectWorkflow reference = getOnlyReference(connectedUser);

        assertThat(reference.isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getConnections())
                    .containsExactly(
                        new ProjectDeploymentWorkflowConnection(administratorConnectionId, "slack", "postMessage1"));
            });
    }

    @Test
    void testEnableReferenceIsSatisfiedByAnInputTheConfigurationProvides() {
        long automationProjectId = createAutomationProject("Provided Input Enable");

        String workflowUuid =
            addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isFalse();

        provideValues(automationProjectId, workflowUuid, Map.of(), Map.of("channel", "#administrator"));

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, true, Environment.PRODUCTION);

        assertThat(getReference(reference.getId()).isEnabled()).isTrue();
        assertThat(fetchRow(reference))
            .get()
            .satisfies(row -> {
                assertThat(row.isEnabled()).isTrue();
                assertThat(row.getInputs()).isEqualTo(Map.of("channel", "#administrator"));
            });
    }

    @Test
    void testAProvidedConnectionIsNeverAmongTheConnectedUsersConnections() {
        long automationProjectId = createAutomationProject("Provided Connection Not Listed");

        String workflowUuid = addAutomation(automationProjectId, SLACK_WORKFLOW_DEFINITION);

        publishAndPinProject(automationProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        long ownedConnectionId = givenOwnedConnection(connectedUser);
        long administratorConnectionId = givenAdministratorConnection();

        provideValues(
            automationProjectId, workflowUuid, Map.of("slack", administratorConnectionId), Map.of());

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(getConnectionIds(reference)).containsExactly(administratorConnectionId);

        assertThat(connectedUserConnectionFacade.getConnections(connectedUser.getId(), "slack", List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactly(ownedConnectionId);
        assertThat(connectedUserConnectionFacade.getConnections(connectedUser.getId(), null, List.of()))
            .extracting(ConnectionDTO::id)
            .containsExactly(ownedConnectionId);
        assertThat(connectedUserConnectionFacade.getOwnedConnectionIds(connectedUser.getId()))
            .containsExactly(ownedConnectionId);
    }

    private long givenAdministratorConnection() {
        long connectionId = NEXT_CONNECTION_ID.getAndIncrement();

        Connection connection = new Connection();

        connection.setComponentName("slack");
        connection.setEnvironmentId(Environment.PRODUCTION.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        when(connectionService.fetchConnection(connectionId)).thenReturn(Optional.of(connection));

        return connectionId;
    }

    private void provideValues(
        long automationProjectId, String workflowUuid, Map<String, Long> providedConnections,
        Map<String, Object> providedInputs) {

        AutomationProjectConfiguration configuration = automationProjectConfigurationService
            .getConfigurations(automationProjectId, Environment.PRODUCTION.ordinal())
            .stream()
            .findFirst()
            .orElseThrow();

        automationProjectConfigurationService.updateWorkflowProvidedValues(
            configuration.getId(), UUID.fromString(workflowUuid), providedConnections, providedInputs);
    }

    private String addAutomation(long automationProjectId, String definition) {
        return automationProjectFacade.createProjectWorkflow(automationProjectId, definition, null);
    }

    private String addHiddenAutomation(long automationProjectId) {
        return automationProjectFacade.createProjectWorkflow(
            automationProjectId, SLACK_WORKFLOW_DEFINITION, HIDDEN_PERMISSION_EXPRESSION);
    }

    private long createAutomationProject(String name) {
        return automationProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null);
    }

    private ConnectedUser createConnectedUser(Environment environment) {
        return connectedUserService.createConnectedUser(newExternalUserId(), environment);
    }

    private Optional<ProjectDeploymentWorkflow> fetchRow(ConnectedUserProjectWorkflow reference) {
        ProjectDeployment projectDeployment = getDeployment(reference);

        return projectWorkflowService
            .fetchProjectWorkflow(
                projectDeployment.getProjectId(), projectDeployment.getProjectVersion(),
                reference.getAutomationUuid())
            .flatMap(projectWorkflow -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(
                projectDeployment.getId(), projectWorkflow.getWorkflowId()));
    }

    private List<Long> getConnectionIds(ConnectedUserProjectWorkflow reference) {
        return fetchRow(reference)
            .map(ProjectDeploymentWorkflow::getConnections)
            .orElseThrow()
            .stream()
            .map(ProjectDeploymentWorkflowConnection::getConnectionId)
            .toList();
    }

    private ProjectDeployment getDeployment(ConnectedUserProjectWorkflow reference) {
        return projectDeploymentService.getProjectDeployment(reference.getProjectDeploymentId());
    }

    private ConnectedUserProjectWorkflow getOnlyReference(ConnectedUser connectedUser) {
        List<ConnectedUserProjectWorkflow> references = connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(
            connectedUser.getId());

        assertThat(references).hasSize(1);

        return references.getFirst();
    }

    private ConnectedUserProjectWorkflow getReference(long id) {
        return connectedUserProjectWorkflowRepository.findById(id)
            .orElseThrow();
    }

    private String getWorkflowId(long automationProjectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(automationProjectId, projectVersion, workflowUuid)
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow();
    }

    private long givenOwnedConnection(ConnectedUser connectedUser) {
        long connectionId = NEXT_CONNECTION_ID.getAndIncrement();

        registerEmbeddedConnection(connectionId, connectedUser.getEnvironment());

        connectedUserConnectionService.create(Objects.requireNonNull(connectedUser.getId()), connectionId);

        return connectionId;
    }

    private void registerEmbeddedConnection(long connectionId, Environment environment) {
        Connection connection = new Connection();

        connection.setEnvironmentId(environment.ordinal());
        connection.setId(connectionId);
        connection.setType(PlatformType.EMBEDDED);

        embeddedConnections.put(connectionId, connection);
    }

    private static String newExternalUserId() {
        return "reference-facade-user-" + UUID.randomUUID();
    }

    private void runWhileTheConnectedUsersProjectIsLocked(
        long connectedUserProjectId, Runnable write, Runnable assertWhileBlocked) throws Exception {

        CountDownLatch lockedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        try {
            Future<?> lockFuture = executorService.submit(() -> transactionTemplate.executeWithoutResult(status -> {
                connectUserProjectRepository.findByIdForUpdate(connectedUserProjectId)
                    .orElseThrow();

                lockedLatch.countDown();

                awaitLatch(releaseLatch);
            }));

            assertThat(lockedLatch.await(60, TimeUnit.SECONDS)).isTrue();

            Future<?> writeFuture = executorService.submit(write);

            assertThatThrownBy(() -> writeFuture.get(2, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

            assertWhileBlocked.run();

            releaseLatch.countDown();

            lockFuture.get(60, TimeUnit.SECONDS);
            writeFuture.get(60, TimeUnit.SECONDS);
        } finally {
            releaseLatch.countDown();

            executorService.shutdownNow();
        }
    }

    private static void awaitLatch(CountDownLatch countDownLatch) {
        try {
            if (!countDownLatch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release the connected user's project lock");
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException(interruptedException);
        }
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
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

    @Nested
    class ProvisionInputs {

        private static final String SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION = """
            {"label":"Post","inputs":[{"name":"channel","label":"Channel","type":"string","required":true}],
             "triggers":[],"tasks":[{"name":"postMessage1","type":"slack/v1/postMessage","parameters":{}}]}
            """;

        @MockitoBean
        private ConnectedUserConnectionFacade connectedUserConnectionFacade;

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

            stubEntitledSlackConnection(777L);
        }

        @Test
        void testProvisionWithTheRequiredInputsEnablesTheReferenceAndStoresThem() {
            String automationUuid = publishChannelInputWorkflow("Provision With Inputs");

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#alerts"));

            assertThat(reference.isEnabled()).isTrue();
            assertThat(getReferenceRow(reference)).satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
                assertThat(projectDeploymentWorkflow.getInputs()).isEqualTo(Map.of("channel", "#alerts"));
            });
        }

        @Test
        void testProvisionWithoutTheRequiredInputLeavesTheReferenceDisabled() {
            String automationUuid = publishChannelInputWorkflow("Provision Without Inputs");

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationUuid, Environment.PRODUCTION, Map.of(), null);

            assertThat(reference.isEnabled()).isFalse();
            assertThat(getReferenceRow(reference).isEnabled()).isFalse();
        }

        @Test
        void testProvisioningAnExistingReferenceAgainReplacesItsInputsAndKeepsItsEnabledState() {
            String automationUuid = publishChannelInputWorkflow("Provision Again");

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationUuid, Environment.PRODUCTION, Map.of(), null);

            assertThat(reference.isEnabled()).isFalse();

            ConnectedUserProjectWorkflow provisionedAgain = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationUuid, Environment.PRODUCTION, Map.of(),
                Map.of("channel", "#general"));

            assertThat(provisionedAgain.getId()).isEqualTo(reference.getId());
            assertThat(provisionedAgain.isEnabled()).isFalse();
            assertThat(getReferenceRow(provisionedAgain).getInputs()).isEqualTo(Map.of("channel", "#general"));

            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#ops"));

            assertThat(getReferenceRow(provisionedAgain).getInputs()).isEqualTo(Map.of("channel", "#ops"));
        }

        private ProjectDeploymentWorkflow getReferenceRow(ConnectedUserProjectWorkflow reference) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(reference.getProjectDeploymentId());

            assertThat(projectDeploymentWorkflows).hasSize(1);

            return projectDeploymentWorkflows.getFirst();
        }

        private String publishChannelInputWorkflow(String projectName) {
            long automationProjectId = createAutomationProject(projectName);

            String automationUuid =
                addAutomation(automationProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

            publishAndPinProject(automationProjectId);

            return automationUuid;
        }

        private String addAutomation(long automationProjectId, String definition) {
            return addAutomation(automationProjectId, definition, null);
        }

        private String
            addAutomation(long automationProjectId, String definition, String permissionExpression) {
            return automationProjectFacade.createProjectWorkflow(
                automationProjectId, definition, permissionExpression);
        }

        private long createAutomationProject(String name) {
            return automationProjectFacade.createProject(
                name + " " + UUID.randomUUID(), "", null, List.of(), null);
        }

        private String createConnectedUser() {
            String newExternalUserId = "project-facade-user-" + UUID.randomUUID();

            connectedUserService.createConnectedUser(newExternalUserId, Environment.PRODUCTION);

            return newExternalUserId;
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
    }

    private void publishAndPinProject(long projectId) {
        AutomationProjectConfigurationTestSupport.publishAndPinProject(
            automationProjectFacade, automationProjectConfigurationService, projectId);
    }
}

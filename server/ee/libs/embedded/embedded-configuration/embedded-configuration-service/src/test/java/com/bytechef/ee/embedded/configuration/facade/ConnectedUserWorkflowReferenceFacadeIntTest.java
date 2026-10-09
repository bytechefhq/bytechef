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

import com.bytechef.atlas.configuration.domain.WorkflowTask;
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
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowTemplateNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
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
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserWorkflowReferenceFacadeIntTest.ConnectedUserServiceConfiguration.class
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

    @MockitoSpyBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private AutomationWorkflowProjectPublishedEventListener automationWorkflowProjectPublishedEventListener;

    @Autowired
    private ComponentConnectionFacade componentConnectionFacade;

    @Autowired
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private ConnectUserProjectRepository connectUserProjectRepository;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;

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
    void testTwoUsersReferencingTheSameAutomationWorkflowGetIndependentWiring() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Two Users");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Per Environment");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Existing Reference");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow existingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        clearInvocations(automationWorkflowProjectFacade, connectedUserReferenceDeploymentManager,
            connectedUserService);

        ConnectedUserProjectWorkflow result = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(result.getId()).isEqualTo(existingReference.getId());
        assertThat(result.getProjectDeploymentId()).isEqualTo(existingReference.getProjectDeploymentId());
        assertThat(getReference(existingReference.getId()).getVersion()).isEqualTo(existingReference.getVersion());

        verifyNoInteractions(automationWorkflowProjectFacade, connectedUserService);

        verify(connectedUserReferenceDeploymentManager).getDeployment(existingReference.getProjectDeploymentId());
        verify(connectedUserReferenceDeploymentManager).getLastPublishedVersion(automationWorkflowProjectId);
        verifyNoMoreInteractions(connectedUserReferenceDeploymentManager);
    }

    @Test
    void testGetOrCreateReferenceReResolvesAnExistingReferenceWithRequestedConnections() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Requested Connections");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        long firstConnectionId = givenOwnedConnection(connectedUser);
        long secondConnectionId = givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        long wiredConnectionId = getConnectionIds(reference).getFirst();
        long requestedConnectionId = wiredConnectionId == firstConnectionId ? secondConnectionId : firstConnectionId;

        clearInvocations(automationWorkflowProjectFacade);

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

        verifyNoInteractions(automationWorkflowProjectFacade);
    }

    @Test
    void testMissingConnectionStillCreatesDisabledReferenceAndRethrows() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Missing Connection");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION))
                .isInstanceOf(MissingConnectionException.class)
                .extracting("componentName")
                .isEqualTo("slack");

        ConnectedUserProjectWorkflow savedReference = getOnlyReference(connectedUser);

        assertThat(savedReference.isEnabled()).isFalse();
        assertThat(savedReference.getAutomationWorkflowUuid()).isEqualTo(workflowUuid);
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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Missing Input Provisioning");

        String workflowUuid =
            addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Enable Toggle");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
    void testEnableReferenceRewiresConnectionsWhenPreviouslyMissingConnectionWasFixed() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Rewire");

        String workflowUuid =
            addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Still Missing");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Missing Input Enable");

        String workflowUuid =
            addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Enable Dangling");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Catch Up Dangling");

        addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        connectedUserWorkflowReferenceFacade.enableReference(
            connectedUser.getExternalId(), workflowUuid, false, Environment.PRODUCTION);

        automationWorkflowProjectFacade.deleteProjectWorkflow(workflowUuid);
        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Disable Behind");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.isEnabled()).isTrue();

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
                assertThat(row.getWorkflowId()).isEqualTo(getWorkflowId(automationWorkflowProjectId, 1, workflowUuid));
                assertThat(row.isEnabled()).isFalse();
            });
    }

    @Test
    void testDisableDanglingReferenceRemovesItsRow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Disable Dangling");

        String siblingWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
            .containsExactly(getWorkflowId(automationWorkflowProjectId, 1, siblingWorkflowUuid));
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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Reference");

        String siblingWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
            .containsExactly(getWorkflowId(automationWorkflowProjectId, 1, siblingWorkflowUuid));
    }

    @Test
    void testDeleteDanglingReferenceAlsoRemovesItsRow() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Dangling");

        String siblingWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);
        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
            .containsExactly(getWorkflowId(automationWorkflowProjectId, 1, siblingWorkflowUuid));
    }

    @Test
    void testUpdateReferenceInputsWritesTheRowUnderTheConnectedUsersLock() throws Exception {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Inputs Lock");

        String workflowUuid =
            addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
    void testUpdateReferenceInputsRefusesADanglingReference() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Inputs Dangling");

        String workflowUuid =
            addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Delete Lock");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

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
        long automationWorkflowProjectId = createAutomationWorkflowProject("Visible Template");

        String visibleWorkflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        addHiddenAutomationWorkflow(automationWorkflowProjectId);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), visibleWorkflowUuid, Environment.PRODUCTION);

        assertThat(reference.getAutomationWorkflowUuid()).isEqualTo(visibleWorkflowUuid);
        assertThat(reference.isEnabled()).isTrue();
    }

    @Test
    void testGetOrCreateReferenceRejectsATemplateHiddenByThePermissionExpression() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Hidden Template");

        addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String hiddenWorkflowUuid = addHiddenAutomationWorkflow(automationWorkflowProjectId);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        clearInvocations(connectedUserReferenceDeploymentManager);

        assertThatThrownBy(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), hiddenWorkflowUuid, Environment.PRODUCTION))
                .isInstanceOf(AutomationWorkflowTemplateNotVisibleException.class);

        verifyNoInteractions(connectedUserReferenceDeploymentManager);

        assertThat(connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())).isEmpty();
        assertThat(projectDeploymentService.fetchProjectDeploymentByName(
            automationWorkflowProjectId, "__EMBEDDED__" + connectedUser.getExternalId() + "__PRODUCTION")).isEmpty();
    }

    @Test
    void testHiddenTemplateRejectionIsIndistinguishableFromAnUnknownUuid() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Indistinguishable");

        addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        String hiddenWorkflowUuid = addHiddenAutomationWorkflow(automationWorkflowProjectId);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        String unknownWorkflowUuid = UUID.randomUUID()
            .toString();

        Throwable hiddenThrowable = catchThrowable(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), hiddenWorkflowUuid, Environment.PRODUCTION));
        Throwable unknownThrowable = catchThrowable(() -> connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), unknownWorkflowUuid, Environment.PRODUCTION));

        assertThat(hiddenThrowable).isExactlyInstanceOf(AutomationWorkflowTemplateNotVisibleException.class);
        assertThat(unknownThrowable).isExactlyInstanceOf(AutomationWorkflowTemplateNotVisibleException.class);
        assertThat(hiddenThrowable).hasMessage("Not a published automation workflow template: " + hiddenWorkflowUuid);
        assertThat(unknownThrowable).hasMessage("Not a published automation workflow template: " + unknownWorkflowUuid);
    }

    @Test
    void testAlreadyProvisionedReferenceIsReturnedWithoutConsultingTheAutomationWorkflows() {
        long automationWorkflowProjectId = createAutomationWorkflowProject("Narrowed Template");

        String workflowUuid = addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION);

        automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

        ConnectedUser connectedUser = createConnectedUser(Environment.PRODUCTION);

        givenOwnedConnection(connectedUser);

        ConnectedUserProjectWorkflow existingReference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        when(embeddedPermissionEvaluator.evaluate(any(), any()))
            .thenReturn(false);

        clearInvocations(automationWorkflowProjectFacade);

        ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
            connectedUser.getExternalId(), workflowUuid, Environment.PRODUCTION);

        assertThat(reference.getId()).isEqualTo(existingReference.getId());
        assertThat(reference.isEnabled()).isTrue();

        verifyNoInteractions(automationWorkflowProjectFacade);

        assertThat(automationWorkflowProjectFacade.getPublishedProjects(
            connectedUser.getExternalId(), Environment.PRODUCTION))
                .flatMap(AutomationWorkflowProjectDTO::workflowTemplates)
                .extracting(ConnectedUserWorkflowTemplateDTO::workflowUuid)
                .doesNotContain(workflowUuid);
    }

    private String addAutomationWorkflow(long automationWorkflowProjectId, String definition) {
        return automationWorkflowProjectFacade.createProjectWorkflow(automationWorkflowProjectId, definition, null);
    }

    private String addHiddenAutomationWorkflow(long automationWorkflowProjectId) {
        return automationWorkflowProjectFacade.createProjectWorkflow(
            automationWorkflowProjectId, SLACK_WORKFLOW_DEFINITION, HIDDEN_PERMISSION_EXPRESSION);
    }

    private long createAutomationWorkflowProject(String name) {
        return automationWorkflowProjectFacade.createProject(
            name + " " + UUID.randomUUID(), "", null, List.of(), null, null);
    }

    private ConnectedUser createConnectedUser(Environment environment) {
        return connectedUserService.createConnectedUser(newExternalUserId(), environment);
    }

    private Optional<ProjectDeploymentWorkflow> fetchRow(ConnectedUserProjectWorkflow reference) {
        ProjectDeployment projectDeployment = getDeployment(reference);

        return projectWorkflowService
            .fetchProjectWorkflow(
                projectDeployment.getProjectId(), projectDeployment.getProjectVersion(),
                reference.getAutomationWorkflowUuid())
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

    private String getWorkflowId(long automationWorkflowProjectId, int projectVersion, String workflowUuid) {
        return projectWorkflowService.fetchProjectWorkflow(automationWorkflowProjectId, projectVersion, workflowUuid)
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
        storedReference.setDanglingReason("Removed from the automation workflow project on redeploy");
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
            String automationWorkflowUuid = publishChannelInputWorkflow("Provision With Inputs");

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationWorkflowUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#alerts"));

            assertThat(reference.isEnabled()).isTrue();
            assertThat(getReferenceRow(reference)).satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.isEnabled()).isTrue();
                assertThat(projectDeploymentWorkflow.getInputs()).isEqualTo(Map.of("channel", "#alerts"));
            });
        }

        @Test
        void testProvisionWithoutTheRequiredInputLeavesTheReferenceDisabled() {
            String automationWorkflowUuid = publishChannelInputWorkflow("Provision Without Inputs");

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationWorkflowUuid, Environment.PRODUCTION, Map.of(), null);

            assertThat(reference.isEnabled()).isFalse();
            assertThat(getReferenceRow(reference).isEnabled()).isFalse();
        }

        @Test
        void testProvisioningAnExistingReferenceAgainReplacesItsInputsAndKeepsItsEnabledState() {
            String automationWorkflowUuid = publishChannelInputWorkflow("Provision Again");

            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationWorkflowUuid, Environment.PRODUCTION, Map.of(), null);

            assertThat(reference.isEnabled()).isFalse();

            ConnectedUserProjectWorkflow provisionedAgain = connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationWorkflowUuid, Environment.PRODUCTION, Map.of(),
                Map.of("channel", "#general"));

            assertThat(provisionedAgain.getId()).isEqualTo(reference.getId());
            assertThat(provisionedAgain.isEnabled()).isFalse();
            assertThat(getReferenceRow(provisionedAgain).getInputs()).isEqualTo(Map.of("channel", "#general"));

            connectedUserWorkflowReferenceFacade.getOrCreateReference(
                externalUserId, automationWorkflowUuid, Environment.PRODUCTION, Map.of(), Map.of("channel", "#ops"));

            assertThat(getReferenceRow(provisionedAgain).getInputs()).isEqualTo(Map.of("channel", "#ops"));
        }

        private ProjectDeploymentWorkflow getReferenceRow(ConnectedUserProjectWorkflow reference) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(reference.getProjectDeploymentId());

            assertThat(projectDeploymentWorkflows).hasSize(1);

            return projectDeploymentWorkflows.getFirst();
        }

        private String publishChannelInputWorkflow(String projectName) {
            long automationWorkflowProjectId = createAutomationWorkflowProject(projectName);

            String automationWorkflowUuid =
                addAutomationWorkflow(automationWorkflowProjectId, SLACK_WORKFLOW_WITH_CHANNEL_INPUT_DEFINITION);

            automationWorkflowProjectFacade.publishProject(automationWorkflowProjectId);

            return automationWorkflowUuid;
        }

        private String addAutomationWorkflow(long automationWorkflowProjectId, String definition) {
            return addAutomationWorkflow(automationWorkflowProjectId, definition, null);
        }

        private String
            addAutomationWorkflow(long automationWorkflowProjectId, String definition, String permissionExpression) {
            return automationWorkflowProjectFacade.createProjectWorkflow(
                automationWorkflowProjectId, definition, permissionExpression);
        }

        private long createAutomationWorkflowProject(String name) {
            return automationWorkflowProjectFacade.createProject(
                name + " " + UUID.randomUUID(), "", null, List.of(), null, null);
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
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityExpressionHandler;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationRowFacade;
import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRowFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorageScopeDeleter.ScopeData;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.expression.MethodSecurityExpressionContributor;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
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
        IntegrationInstanceFacadeIntTest.ConnectedUserServiceConfiguration.class,
        IntegrationInstanceFacadeIntTest.MethodSecurityConfiguration.class,
        IntegrationInstanceFacadeIntTest.RecordingDataStorageConfiguration.class
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
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionFacade.class,
    ComponentDefinitionService.class, ConnectionDefinitionService.class, ConnectionFacade.class,
    ConnectionLifecycleFacade.class, ConnectionService.class, EnvironmentService.class, GitHubProxyClient.class,
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationComponentService.class,
    McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpIntegrationInstanceWorkflowService.class, McpAutomationRowFacade.class, McpIntegrationInstanceRowFacade.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PermissionService.class, PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class IntegrationInstanceFacadeIntTest {

    private static final String DISABLED_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Disabled trigger","inputs":[],"triggers":[\
        {"name":"trigger_1","type":"slack/v1/newMessage","disabled":true},\
        {"name":"trigger_2","type":"slack/v1/newMessage"}],\
        "tasks":[]}
        """;

    private static final String INTERNAL_ONLY_INPUT_WORKFLOW_DEFINITION = """
        {"label":"Internal only input","inputs":[\
        {"name":"accountId","type":"string","internalOnly":true},{"name":"region","type":"string"}],\
        "triggers":[{"name":"trigger_1","type":"slack/v1/newMessage","parameters":{"account":"=accountId"}}],\
        "tasks":[]}
        """;

    private static final String TWO_TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"Two triggers","inputs":[],"triggers":[\
        {"name":"trigger_1","type":"slack/v1/newMessage"},{"name":"trigger_2","type":"slack/v1/newMessage"}],\
        "tasks":[]}
        """;

    private static final String STATIC_WEBHOOK_WORKFLOW_DEFINITION = """
        {"label":"Static webhook","inputs":[],"triggers":[\
        {"name":"trigger_1","type":"webhook/v1/awaitWorkflowAndRespond"}],\
        "tasks":[]}
        """;

    private static final String WORKFLOW_DEFINITION = """
        {"label":"Sync contacts","inputs":[],"triggers":[],"tasks":[]}
        """;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private ConnectionFacade connectionFacade;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoSpyBean
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RecordingDataStorage recordingDataStorage;

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkflowService workflowService;

    private ConnectedUser connectedUserA;
    private ConnectedUser developmentConnectedUserA;
    private long connectionAId;
    private String externalUserAId;
    private long integrationId;
    private long integrationInstanceAId;
    private long integrationInstanceBId;
    private long integrationInstanceConfigurationId;
    private String workflowId;

    @BeforeEach
    void beforeEach() {
        recordingDataStorage.reset();

        connectionAId = UUID.randomUUID()
            .getMostSignificantBits() & Long.MAX_VALUE;

        externalUserAId = "external-user-a-" + UUID.randomUUID();

        connectedUserA = connectedUserService.createConnectedUser(externalUserAId, Environment.PRODUCTION);
        developmentConnectedUserA = connectedUserService.createConnectedUser(
            externalUserAId, Environment.DEVELOPMENT);

        ConnectedUser connectedUserB = connectedUserService.createConnectedUser(
            "external-user-b-" + UUID.randomUUID(), Environment.PRODUCTION);

        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("Slack " + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationId = integration.getId();

        Workflow workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        workflowId = workflow.getId();

        integrationWorkflowService.addWorkflow(integration.getId(), 1, workflowId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Slack");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationId = integrationInstanceConfiguration.getId();

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfigurationId, true);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfiguration.getId());
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        IntegrationInstance integrationInstanceA = integrationInstanceService.create(
            connectedUserA.getId(), connectionAId, integrationInstanceConfiguration.getId());

        integrationInstanceAId = integrationInstanceA.getId();

        IntegrationInstance integrationInstanceB = integrationInstanceService.create(
            connectedUserB.getId(), 42L, integrationInstanceConfiguration.getId());

        integrationInstanceBId = integrationInstanceB.getId();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        connectedUserConnectionService.deleteByConnectionId(connectionAId);
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesConnectedUserOnAnotherUsersInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceBId, workflowId, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId)).isEmpty();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesConnectedUserFromAnotherEnvironment() {
        authenticate(createConnectedUserAuthentication(Environment.DEVELOPMENT, developmentConnectedUserA));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, workflowId, true))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId)).isEmpty();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowsConnectedUserOnOwnInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceAId, workflowId, true);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isTrue());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowsTenantAdminOnAnyInstance() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceBId, workflowId, true);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.isEnabled())
                .isTrue());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniesCallerWhoIsNeitherTenantAdminNorConnectedUser() {
        authenticate(
            new UsernamePasswordAuthenticationToken(
                "user@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, workflowId, true))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId)).isEmpty();
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDisablesTheAlreadyEnabledTriggersWhenALaterTriggerFailsToEnable() {
        Workflow twoTriggerWorkflow = workflowService.create(
            TWO_TRIGGER_WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String twoTriggerWorkflowId = twoTriggerWorkflow.getId();

        integrationWorkflowService.addWorkflow(integrationId, 1, twoTriggerWorkflowId);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(twoTriggerWorkflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        IllegalStateException enableFailure = new IllegalStateException("trigger_2 failed to enable");

        doThrow(enableFailure)
            .when(triggerLifecycleFacade)
            .executeTriggerEnable(
                eq(twoTriggerWorkflowId),
                argThat(workflowExecutionId -> "trigger_2".equals(workflowExecutionId.getTriggerName())),
                any(), anyMap(), any(), anyString(), anyLong());

        authenticate(createTenantAdminAuthentication());

        assertThatThrownBy(() -> integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, twoTriggerWorkflowId, true))
                .isSameAs(enableFailure);

        verify(triggerLifecycleFacade).executeTriggerDisable(
            eq(twoTriggerWorkflowId),
            argThat(workflowExecutionId -> "trigger_1".equals(workflowExecutionId.getTriggerName())),
            any(), anyMap(), any());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(
            eq(twoTriggerWorkflowId),
            argThat(workflowExecutionId -> "trigger_2".equals(workflowExecutionId.getTriggerName())),
            any(), anyMap(), any());
    }

    @Test
    void testEnableSkipsDisabledTrigger() {
        String disabledTriggerWorkflowId = createDisabledTriggerWorkflow();

        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, disabledTriggerWorkflowId, true);

        verify(triggerLifecycleFacade).executeTriggerEnable(
            eq(disabledTriggerWorkflowId),
            argThat(workflowExecutionId -> "trigger_2".equals(workflowExecutionId.getTriggerName())),
            any(), anyMap(), any(), anyString(), anyLong());
        verify(triggerLifecycleFacade, never()).executeTriggerEnable(
            eq(disabledTriggerWorkflowId),
            argThat(workflowExecutionId -> "trigger_1".equals(workflowExecutionId.getTriggerName())),
            any(), anyMap(), any(), anyString(), anyLong());
    }

    @Test
    void testDisableSkipsDisabledTrigger() {
        String disabledTriggerWorkflowId = createDisabledTriggerWorkflow();

        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, disabledTriggerWorkflowId, true);

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, disabledTriggerWorkflowId, false);

        verify(triggerLifecycleFacade).executeTriggerDisable(
            eq(disabledTriggerWorkflowId),
            argThat(workflowExecutionId -> "trigger_2".equals(workflowExecutionId.getTriggerName())),
            any(), anyMap(), any());
        verify(triggerLifecycleFacade, never()).executeTriggerDisable(
            eq(disabledTriggerWorkflowId),
            argThat(workflowExecutionId -> "trigger_1".equals(workflowExecutionId.getTriggerName())),
            any(), anyMap(), any());
    }

    private String createDisabledTriggerWorkflow() {
        Workflow disabledTriggerWorkflow = workflowService.create(
            DISABLED_TRIGGER_WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String disabledTriggerWorkflowId = disabledTriggerWorkflow.getId();

        integrationWorkflowService.addWorkflow(integrationId, 1, disabledTriggerWorkflowId);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(disabledTriggerWorkflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        return disabledTriggerWorkflowId;
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowDeniesConnectedUserOnAnotherUsersInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceBId, workflowId, Map.of("key", "value")))
                .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId)).isEmpty();
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowsConnectedUserOnOwnInstance() {
        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceAId, workflowId, Map.of("key", "value"));

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceAId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.getInputs())
                .isEqualTo(Map.of("key", "value")));
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowKeepsTheStoredInternalOnlyInputOverAConnectedUsersValue() {
        String internalOnlyInputWorkflowId = createInternalOnlyInputWorkflow(Map.of());

        storeIntegrationInstanceWorkflowInputs(
            internalOnlyInputWorkflowId, Map.of("accountId", "vendor-account", "region", "eu"));

        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceAId, internalOnlyInputWorkflowId,
            Map.of("accountId", "user-account", "region", "us"));

        assertThat(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(
                integrationInstanceAId, internalOnlyInputWorkflowId)
                .getInputs()).isEqualTo(Map.of("accountId", "vendor-account", "region", "us"));
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowDropsAnInternalOnlyInputSetByAConnectedUser() {
        String internalOnlyInputWorkflowId = createInternalOnlyInputWorkflow(Map.of());

        authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceAId, internalOnlyInputWorkflowId,
            Map.of("accountId", "user-account", "region", "us"));

        assertThat(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(
                integrationInstanceAId, internalOnlyInputWorkflowId)
                .getInputs()).isEqualTo(Map.of("region", "us"));
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowLetsTheApiKeyAuthenticatedVendorSetAnInternalOnlyInput() {
        String internalOnlyInputWorkflowId = createInternalOnlyInputWorkflow(Map.of());

        authenticate(
            new EmbeddedApiKeyAuthenticationToken(
                Environment.PRODUCTION.ordinal(), connectedUserA.getId(), new User(externalUserAId, "", List.of()),
                true));

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceAId, internalOnlyInputWorkflowId,
            Map.of("accountId", "vendor-account", "region", "us"));

        assertThat(
            integrationInstanceWorkflowService.getIntegrationInstanceWorkflow(
                integrationInstanceAId, internalOnlyInputWorkflowId)
                .getInputs()).isEqualTo(Map.of("accountId", "vendor-account", "region", "us"));
    }

    @Test
    void testEnableIntegrationInstanceWorkflowPassesTheConfigurationValueOfAnInternalOnlyInputToTheTrigger() {
        String internalOnlyInputWorkflowId = createInternalOnlyInputWorkflow(Map.of("accountId", "vendor-account"));

        storeIntegrationInstanceWorkflowInputs(internalOnlyInputWorkflowId, Map.of("accountId", "user-account"));

        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(
            integrationInstanceAId, internalOnlyInputWorkflowId, true);

        verify(triggerLifecycleFacade).executeTriggerEnable(
            eq(internalOnlyInputWorkflowId), any(), any(), eq(Map.of("account", "vendor-account")), any(),
            anyString(), anyLong());
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowsTenantAdminOnAnyInstance() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.updateIntegrationInstanceWorkflow(
            integrationInstanceBId, workflowId, Map.of("key", "value"));

        assertThat(fetchIntegrationInstanceWorkflow(integrationInstanceBId))
            .hasValueSatisfying(integrationInstanceWorkflow -> assertThat(integrationInstanceWorkflow.getInputs())
                .isEqualTo(Map.of("key", "value")));
    }

    @Test
    void testDeleteIntegrationInstanceDeletesItsDataStorageAfterCommit() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceAId, workflowId, true);

        IntegrationInstanceWorkflow integrationInstanceWorkflow = fetchIntegrationInstanceWorkflow(
            integrationInstanceAId).orElseThrow();

        integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceAId);

        assertThat(recordingDataStorage.getDeletedScopeDataList()).containsExactlyInAnyOrder(
            new ScopeData(DataStorageScope.PRINCIPAL, String.valueOf(integrationInstanceAId), PlatformType.EMBEDDED),
            new ScopeData(
                DataStorageScope.WORKFLOW, String.valueOf(integrationInstanceWorkflow.getId()),
                PlatformType.EMBEDDED));
    }

    @Test
    void testDeleteIntegrationInstanceDeletesItsUnusedConnection() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceAId);

        verify(connectionService).delete(connectionAId);

        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserA.getId())).isEmpty();
    }

    @Test
    void testDeleteIntegrationInstanceHandsItsConnectionInUseToTheConnectedUser() {
        authenticate(createTenantAdminAuthentication());

        when(connectionFacade.isConnectionUsed(connectionAId)).thenReturn(true);

        integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceAId);

        verify(connectionService, never()).delete(anyLong());

        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserA.getId()))
            .containsExactly(connectionAId);
    }

    @Test
    void testDeleteIntegrationInstanceKeepsTheConnectionAnotherInstanceUses() {
        authenticate(createTenantAdminAuthentication());

        integrationInstanceService.create(connectedUserA.getId(), connectionAId, integrationInstanceConfigurationId);

        integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceAId);

        verify(connectionService, never()).delete(anyLong());

        assertThat(connectedUserConnectionService.getConnectionIds(connectedUserA.getId())).isEmpty();
    }

    @Test
    void testFailingDataStorageDoesNotFailTheInstanceDelete() {
        authenticate(createTenantAdminAuthentication());

        recordingDataStorage.setFailing(true);

        integrationInstanceFacade.deleteIntegrationInstance(integrationInstanceAId);

        assertThat(recordingDataStorage.getAttempts()).isPositive();
        assertThat(integrationInstanceService.getIntegrationInstances(List.of(integrationInstanceAId))).isEmpty();
    }

    @Test
    void testGetIntegrationInstanceReturnsTheStaticWebhookUrlOfAWorkflowWithAStaticWebhookTrigger() {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        when(triggerDefinition.getType()).thenReturn(TriggerType.STATIC_WEBHOOK);
        when(triggerDefinitionService.getTriggerDefinition("webhook", 1, "awaitWorkflowAndRespond"))
            .thenReturn(triggerDefinition);

        Workflow staticWebhookWorkflow = workflowService.create(
            STATIC_WEBHOOK_WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String staticWebhookWorkflowId = staticWebhookWorkflow.getId();

        IntegrationWorkflow staticWebhookIntegrationWorkflow = integrationWorkflowService.addWorkflow(
            integrationId, 1, staticWebhookWorkflowId);

        configureIntegrationInstanceAWorkflow(staticWebhookWorkflowId);

        authenticate(createTenantAdminAuthentication());

        IntegrationInstanceWorkflowDTO integrationInstanceWorkflowDTO = getIntegrationInstanceAWorkflowDTO(
            staticWebhookWorkflowId);

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.EMBEDDED, integrationInstanceAId, staticWebhookIntegrationWorkflow.getUuidAsString(),
            "trigger_1");

        assertThat(integrationInstanceWorkflowDTO.staticWebhookUrl()).isEqualTo("/webhooks/" + workflowExecutionId);
    }

    @Test
    void testGetIntegrationInstanceReturnsNoStaticWebhookUrlForAWorkflowWithoutAStaticWebhookTrigger() {
        createIntegrationInstanceAWorkflow(workflowId);

        authenticate(createTenantAdminAuthentication());

        IntegrationInstanceWorkflowDTO integrationInstanceWorkflowDTO = getIntegrationInstanceAWorkflowDTO(workflowId);

        assertThat(integrationInstanceWorkflowDTO.staticWebhookUrl()).isNull();
    }

    @Test
    void testGetIntegrationInstanceReturnsNoStaticWebhookUrlWhenTheIntegrationWorkflowIsMissing() {
        Workflow danglingWorkflow = workflowService.create(
            STATIC_WEBHOOK_WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String danglingWorkflowId = danglingWorkflow.getId();

        configureIntegrationInstanceAWorkflow(danglingWorkflowId);

        authenticate(createTenantAdminAuthentication());

        IntegrationInstanceWorkflowDTO integrationInstanceWorkflowDTO = getIntegrationInstanceAWorkflowDTO(
            danglingWorkflowId);

        assertThat(integrationInstanceWorkflowDTO.staticWebhookUrl()).isNull();
    }

    private void configureIntegrationInstanceAWorkflow(String configuredWorkflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(configuredWorkflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        createIntegrationInstanceAWorkflow(configuredWorkflowId);
    }

    private void createIntegrationInstanceAWorkflow(String configuredWorkflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, configuredWorkflowId);

        integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
            integrationInstanceAId, integrationInstanceConfigurationWorkflow.getId());
    }

    private IntegrationInstanceWorkflowDTO getIntegrationInstanceAWorkflowDTO(String integrationInstanceWorkflowId) {
        IntegrationInstanceDTO integrationInstanceDTO = integrationInstanceFacade.getIntegrationInstance(
            integrationInstanceAId);

        return integrationInstanceDTO.integrationInstanceWorkflows()
            .stream()
            .filter(integrationInstanceWorkflowDTO -> integrationInstanceWorkflowId.equals(
                integrationInstanceWorkflowDTO.workflowId()))
            .findFirst()
            .orElseThrow();
    }

    private String createInternalOnlyInputWorkflow(Map<String, ?> configurationInputs) {
        Workflow internalOnlyInputWorkflow = workflowService.create(
            INTERNAL_ONLY_INPUT_WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        String internalOnlyInputWorkflowId = internalOnlyInputWorkflow.getId();

        integrationWorkflowService.addWorkflow(integrationId, 1, internalOnlyInputWorkflowId);

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(configurationInputs);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(internalOnlyInputWorkflowId);

        integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

        return internalOnlyInputWorkflowId;
    }

    private void storeIntegrationInstanceWorkflowInputs(String workflowId, Map<String, ?> inputs) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, workflowId);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstanceAId, integrationInstanceConfigurationWorkflow.getId());

        integrationInstanceWorkflow.setInputs(inputs);

        integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private EmbeddedApiKeyAuthenticationToken createConnectedUserAuthentication(
        Environment environment, ConnectedUser connectedUser) {

        return new EmbeddedApiKeyAuthenticationToken(
            environment.ordinal(), connectedUser.getId(), new User(externalUserAId, "", List.of()), false);
    }

    private Authentication createTenantAdminAuthentication() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        return new UsernamePasswordAuthenticationToken(
            "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(long integrationInstanceId) {
        return integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(integrationInstanceId, workflowId);
    }

    @Nested
    class Provisioning {

        private static final long PROVISIONED_CONNECTED_USER_ID = 5L;
        private static final long PROVISIONED_CONNECTION_ID = 9L;
        private static final long PROVISIONED_INTEGRATION_INSTANCE_ID = 9_000_001L;
        private static final long PREVIOUS_CONNECTION_ID = 101L;
        private static final long NEW_CONNECTION_ID = 202L;

        private String triggerWorkflowId;

        @MockitoBean
        private ConnectedUserConnectionFacade connectedUserConnectionFacade;

        @MockitoBean
        private ConnectedUserIntegrationFacade connectedUserIntegrationFacade;

        @Test
        void testProvisionIntegrationInstanceIsDeniedForConnectedUsersAndNonAdmins() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

            assertThatThrownBy(
                () -> integrationInstanceFacade.provisionIntegrationInstance(1L, 2L, Map.of("key", "v"), List.of()))
                    .isInstanceOf(AccessDeniedException.class);

            authenticate(createNonAdminAuthentication());

            assertThatThrownBy(
                () -> integrationInstanceFacade.provisionIntegrationInstance(1L, 2L, Map.of("key", "v"), List.of()))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(connectedUserIntegrationFacade);
        }

        @Test
        void testProvisionIntegrationInstanceDelegatesForTenantAdmin() {
            authenticate(createTenantAdminAuthentication());

            integrationInstanceFacade.provisionIntegrationInstance(1L, 2L, Map.of("key", "v"), List.of());

            verify(connectedUserIntegrationFacade).provisionIntegrationInstance(
                1L, 2L, Map.of("key", "v"), List.of(), "admin@localhost.com");
        }

        @Test
        void testGetProvisionableIntegrationsIsDeniedForConnectedUsersAndNonAdmins() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

            assertThatThrownBy(() -> integrationInstanceFacade.getProvisionableIntegrations(1L))
                .isInstanceOf(AccessDeniedException.class);

            authenticate(createNonAdminAuthentication());

            assertThatThrownBy(() -> integrationInstanceFacade.getProvisionableIntegrations(1L))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(connectedUserIntegrationFacade);
        }

        @Test
        void testGetProvisionableIntegrationsDelegatesForTenantAdmin() {
            authenticate(createTenantAdminAuthentication());

            integrationInstanceFacade.getProvisionableIntegrations(1L);

            verify(connectedUserIntegrationFacade).getProvisionableIntegrations(1L);
        }

        @Test
        void testGetIntegrationInstanceConnectionReturnsProvisionedConnection() {
            authenticate(createTenantAdminAuthentication());

            givenProvisionedIntegrationInstance();

            ConnectionDTO connectionDTO = ConnectionDTO.builder()
                .id(PROVISIONED_CONNECTION_ID)
                .build();

            when(connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
                PROVISIONED_CONNECTED_USER_ID, null, List.of(PROVISIONED_CONNECTION_ID)))
                    .thenReturn(List.of(connectionDTO));

            assertThat(integrationInstanceFacade.getIntegrationInstanceConnection(PROVISIONED_INTEGRATION_INSTANCE_ID))
                .isEqualTo(connectionDTO);
        }

        @Test
        void testGetIntegrationInstanceConnectionThrowsForSelfActivatedInstance() {
            authenticate(createTenantAdminAuthentication());

            assertThatThrownBy(() -> integrationInstanceFacade.getIntegrationInstanceConnection(integrationInstanceAId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Integration instance id=%s not found".formatted(integrationInstanceAId));

            verifyNoInteractions(connectedUserConnectionFacade);
        }

        @Test
        void testGetIntegrationInstanceConnectionThrowsForUnknownInstance() {
            authenticate(createTenantAdminAuthentication());

            assertThatThrownBy(
                () -> integrationInstanceFacade.getIntegrationInstanceConnection(PROVISIONED_INTEGRATION_INSTANCE_ID))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void testUpdateIntegrationInstanceCredentialsDelegatesForProvisionedInstance() {
            authenticate(createTenantAdminAuthentication());

            givenProvisionedIntegrationInstance();

            integrationInstanceFacade.updateIntegrationInstanceCredentials(
                PROVISIONED_INTEGRATION_INSTANCE_ID, Map.of("token", "new"), 4);

            verify(connectedUserConnectionFacade).updateIntegrationInstanceConnectionCredentials(
                PROVISIONED_CONNECTED_USER_ID, PROVISIONED_CONNECTION_ID, Map.of("token", "new"), 4);
        }

        @Test
        void testUpdateIntegrationInstanceCredentialsThrowsForSelfActivatedInstance() {
            authenticate(createTenantAdminAuthentication());

            Map<String, String> parameters = Map.of("token", "new");

            assertThatThrownBy(
                () -> integrationInstanceFacade.updateIntegrationInstanceCredentials(
                    integrationInstanceAId, parameters, 4))
                        .isInstanceOf(NoSuchElementException.class)
                        .hasMessage("Integration instance id=%s not found".formatted(integrationInstanceAId));

            verify(connectedUserConnectionFacade, never())
                .updateIntegrationInstanceConnectionCredentials(anyLong(), anyLong(), any(), anyInt());
        }

        @Test
        void testIntegrationInstanceCredentialsAreDeniedForConnectedUsersAndNonAdmins() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

            assertThatThrownBy(() -> integrationInstanceFacade.getIntegrationInstanceConnection(1L))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> integrationInstanceFacade.updateIntegrationInstanceCredentials(1L, Map.of("token", "new"), 0))
                    .isInstanceOf(AccessDeniedException.class);

            authenticate(createNonAdminAuthentication());

            assertThatThrownBy(() -> integrationInstanceFacade.getIntegrationInstanceConnection(1L))
                .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                () -> integrationInstanceFacade.updateIntegrationInstanceCredentials(1L, Map.of("token", "new"), 0))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(connectedUserConnectionFacade);
        }

        @Test
        void testUpdateIntegrationInstanceConnectionSwitchesTheConnectionAndRestartsTheTriggers() {
            authenticate(createTenantAdminAuthentication());

            long integrationInstanceId = createProvisionedIntegrationInstanceWithTriggers();

            givenConnectedUserConnection(NEW_CONNECTION_ID, Environment.PRODUCTION);

            integrationInstanceFacade.updateIntegrationInstanceConnection(integrationInstanceId, NEW_CONNECTION_ID);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                integrationInstanceId);

            assertThat(integrationInstance.getConnectionId()).isEqualTo(NEW_CONNECTION_ID);

            verify(triggerLifecycleFacade, times(2)).executeTriggerDisable(
                eq(triggerWorkflowId), any(), any(), anyMap(), eq(PREVIOUS_CONNECTION_ID));
            verify(triggerLifecycleFacade, times(2)).executeTriggerEnable(
                eq(triggerWorkflowId), any(), any(), anyMap(), eq(NEW_CONNECTION_ID), anyString(), anyLong());
            verify(triggerLifecycleFacade, never()).executeTriggerEnable(
                any(), any(), any(), anyMap(), eq(PREVIOUS_CONNECTION_ID), anyString(), anyLong());
        }

        @Test
        void testUpdateIntegrationInstanceConnectionRestoresThePreviousConnectionWhenATriggerFailsToRestart() {
            authenticate(createTenantAdminAuthentication());

            long integrationInstanceId = createProvisionedIntegrationInstanceWithTriggers();

            givenConnectedUserConnection(NEW_CONNECTION_ID, Environment.PRODUCTION);

            IllegalStateException restartFailure = new IllegalStateException("trigger_2 failed to enable");

            doThrow(restartFailure)
                .when(triggerLifecycleFacade)
                .executeTriggerEnable(
                    eq(triggerWorkflowId),
                    argThat(workflowExecutionId -> "trigger_2".equals(workflowExecutionId.getTriggerName())),
                    any(), anyMap(), eq(NEW_CONNECTION_ID), anyString(), anyLong());

            assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceConnection(
                integrationInstanceId, NEW_CONNECTION_ID))
                    .isSameAs(restartFailure);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                integrationInstanceId);

            assertThat(integrationInstance.getConnectionId()).isEqualTo(PREVIOUS_CONNECTION_ID);

            verify(triggerLifecycleFacade).executeTriggerDisable(
                eq(triggerWorkflowId),
                argThat(workflowExecutionId -> "trigger_1".equals(workflowExecutionId.getTriggerName())),
                any(), anyMap(), eq(NEW_CONNECTION_ID));
            verify(triggerLifecycleFacade, times(2)).executeTriggerEnable(
                eq(triggerWorkflowId), any(), any(), anyMap(), eq(PREVIOUS_CONNECTION_ID), anyString(), anyLong());
        }

        @Test
        void testUpdateIntegrationInstanceConnectionOfADisabledInstanceLeavesTheTriggersAlone() {
            authenticate(createTenantAdminAuthentication());

            long integrationInstanceId = createProvisionedIntegrationInstanceWithTriggers();

            integrationInstanceService.updateEnabled(integrationInstanceId, false);

            givenConnectedUserConnection(NEW_CONNECTION_ID, Environment.PRODUCTION);

            integrationInstanceFacade.updateIntegrationInstanceConnection(integrationInstanceId, NEW_CONNECTION_ID);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                integrationInstanceId);

            assertThat(integrationInstance.getConnectionId()).isEqualTo(NEW_CONNECTION_ID);

            verifyNoInteractions(triggerLifecycleFacade);
        }

        @Test
        void testUpdateIntegrationInstanceConnectionRejectsAConnectionTheConnectedUserDoesNotHave() {
            authenticate(createTenantAdminAuthentication());

            long integrationInstanceId = createProvisionedIntegrationInstanceWithTriggers();

            assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceConnection(
                integrationInstanceId, NEW_CONNECTION_ID))
                    .isInstanceOf(IllegalArgumentException.class);

            IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
                integrationInstanceId);

            assertThat(integrationInstance.getConnectionId()).isEqualTo(PREVIOUS_CONNECTION_ID);

            verifyNoInteractions(triggerLifecycleFacade);
        }

        @Test
        void testUpdateIntegrationInstanceConnectionRejectsAConnectionFromAnotherEnvironment() {
            authenticate(createTenantAdminAuthentication());

            long integrationInstanceId = createProvisionedIntegrationInstanceWithTriggers();

            givenConnectedUserConnection(NEW_CONNECTION_ID, Environment.DEVELOPMENT);

            assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceConnection(
                integrationInstanceId, NEW_CONNECTION_ID))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(triggerLifecycleFacade);
        }

        @Test
        void testUpdateIntegrationInstanceConnectionThrowsForSelfActivatedInstance() {
            authenticate(createTenantAdminAuthentication());

            assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceConnection(
                integrationInstanceAId, NEW_CONNECTION_ID))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Integration instance id=%s not found".formatted(integrationInstanceAId));

            verifyNoInteractions(connectedUserConnectionFacade);
        }

        @Test
        void testUpdateIntegrationInstanceConnectionIsDeniedForConnectedUsersAndNonAdmins() {
            authenticate(createConnectedUserAuthentication(Environment.PRODUCTION, connectedUserA));

            assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceConnection(1L, 2L))
                .isInstanceOf(AccessDeniedException.class);

            authenticate(createNonAdminAuthentication());

            assertThatThrownBy(() -> integrationInstanceFacade.updateIntegrationInstanceConnection(1L, 2L))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(connectedUserConnectionFacade);
        }

        private long createProvisionedIntegrationInstanceWithTriggers() {
            Workflow triggerWorkflow = workflowService.create(
                TWO_TRIGGER_WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            triggerWorkflowId = triggerWorkflow.getId();

            integrationWorkflowService.addWorkflow(integrationId, 1, triggerWorkflowId);

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setEnabled(true);
            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfigurationId);
            integrationInstanceConfigurationWorkflow.setWorkflowId(triggerWorkflowId);

            integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
                integrationInstanceConfigurationWorkflow);

            IntegrationInstance integrationInstance = integrationInstanceService.create(
                connectedUserA.getId(), PREVIOUS_CONNECTION_ID, integrationInstanceConfigurationId,
                "admin@localhost.com");

            IntegrationInstanceWorkflow integrationInstanceWorkflow =
                integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                    integrationInstance.getId(), integrationInstanceConfigurationWorkflow.getId());

            integrationInstanceWorkflowService.updateEnabled(integrationInstanceWorkflow.getId(), true);

            return integrationInstance.getId();
        }

        private void givenConnectedUserConnection(long connectionId, Environment environment) {
            ConnectionDTO connectionDTO = ConnectionDTO.builder()
                .environmentId(environment.ordinal())
                .id(connectionId)
                .build();

            when(connectedUserConnectionFacade.getConnectedUserConnectionsForAdmin(
                connectedUserA.getId(), "slack", List.of(connectionId)))
                    .thenReturn(List.of(connectionDTO));
        }

        private Authentication createNonAdminAuthentication() {
            when(permissionService.isTenantAdmin()).thenReturn(false);

            return new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        }

        private void givenProvisionedIntegrationInstance() {
            IntegrationInstance integrationInstance = new IntegrationInstance();

            integrationInstance.setConnectedUserId(PROVISIONED_CONNECTED_USER_ID);
            integrationInstance.setConnectionId(PROVISIONED_CONNECTION_ID);
            integrationInstance.setId(PROVISIONED_INTEGRATION_INSTANCE_ID);
            integrationInstance.setProvisionedBy("admin");

            doReturn(Optional.of(integrationInstance))
                .when(integrationInstanceService)
                .fetchIntegrationInstance(PROVISIONED_INTEGRATION_INSTANCE_ID);
        }
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    @Configuration
    static class RecordingDataStorageConfiguration {

        @Bean
        RecordingDataStorage recordingDataStorage() {
            return new RecordingDataStorage();
        }
    }

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {

        @Bean
        static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            @Lazy PermissionService permissionService,
            ObjectProvider<MethodSecurityExpressionContributor> methodSecurityExpressionContributorProvider) {

            return new AutomationMethodSecurityExpressionHandler(
                permissionService, methodSecurityExpressionContributorProvider);
        }
    }
}

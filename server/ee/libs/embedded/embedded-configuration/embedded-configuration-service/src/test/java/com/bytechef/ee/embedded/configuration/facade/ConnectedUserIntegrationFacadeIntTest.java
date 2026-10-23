/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO.ProvisionableIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserIntegrationFacadeIntTest.ConnectedUserServiceConfiguration.class
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
    JobFacade.class, JobService.class, McpComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, ProjectFacade.class,
    TaskDispatcherDefinitionService.class, TaskExecutionService.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ConnectedUserIntegrationFacadeIntTest {

    private static final long CONNECTION_ID = 42L;
    private static final long MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID = 501L;
    private static final long MCP_SERVER_ID = 601L;

    private static final String TRIGGER_WORKFLOW_DEFINITION = """
        {"label":"%s","triggers":[{"name":"trigger_1","type":"slack/v1/newMessage","parameters":{}}],\
        "tasks":[]}""";

    @Autowired
    private ConnectedUserIntegrationFacade connectedUserIntegrationFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Autowired
    private IntegrationInstanceService integrationInstanceService;

    @Autowired
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationWorkflowService integrationWorkflowService;

    @Autowired
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @Autowired
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkflowService workflowService;

    private String disabledWorkflowId;
    private String enabledWorkflowId;
    private String enabledWorkflowUuid;
    private long integrationId;
    private long integrationInstanceId;
    private String otherExternalUserId;
    private String ownerExternalUserId;

    @BeforeEach
    void beforeEach() {
        ownerExternalUserId = "owner-" + UUID.randomUUID();
        otherExternalUserId = "other-" + UUID.randomUUID();

        ConnectedUser ownerConnectedUser = connectedUserService.createConnectedUser(
            ownerExternalUserId, Environment.PRODUCTION);

        connectedUserService.createConnectedUser(otherExternalUserId, Environment.PRODUCTION);

        Integration integration = new Integration();

        integration.setComponentName("slack");
        integration.setName("Slack " + UUID.randomUUID());

        integration = integrationService.create(integration);

        integrationId = integration.getId();

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integration.getId());
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("Slack");

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), true);

        IntegrationInstance integrationInstance = integrationInstanceService.create(
            ownerConnectedUser.getId(), CONNECTION_ID, integrationInstanceConfiguration.getId());

        integrationInstanceId = integrationInstance.getId();

        IntegrationWorkflow enabledIntegrationWorkflow = addWorkflow(integration.getId(), "workflow-enabled");

        enabledWorkflowId = enabledIntegrationWorkflow.getWorkflowId();
        enabledWorkflowUuid = enabledIntegrationWorkflow.getUuidAsString();

        IntegrationWorkflow disabledIntegrationWorkflow = addWorkflow(integration.getId(), "workflow-disabled");

        disabledWorkflowId = disabledIntegrationWorkflow.getWorkflowId();

        createIntegrationInstanceWorkflow(integrationInstanceConfiguration.getId(), enabledWorkflowId, true);
        createIntegrationInstanceWorkflow(integrationInstanceConfiguration.getId(), disabledWorkflowId, false);
    }

    @Test
    void testDeleteAllowedForOwner() {
        integrationInstanceService.updateEnabled(integrationInstanceId, false);

        connectedUserIntegrationFacade.deleteIntegrationInstance(ownerExternalUserId, integrationInstanceId);

        assertThat(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId))
            .isEmpty();
        assertThat(integrationInstanceService.getIntegrationInstances(List.of(integrationInstanceId))).isEmpty();
    }

    @Test
    void testDeleteDisablesTheEnabledWorkflowsOfAnEnabledInstanceBeforeDeletingThem() {
        List<Integer> integrationInstanceWorkflowCountsAtTriggerDisable = new ArrayList<>();

        doAnswer(invocation -> {
            integrationInstanceWorkflowCountsAtTriggerDisable.add(
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId)
                    .size());

            return null;
        }).when(triggerLifecycleFacade)
            .executeTriggerDisable(any(), any(), any(), any(), any());

        connectedUserIntegrationFacade.deleteIntegrationInstance(ownerExternalUserId, integrationInstanceId);

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(triggerLifecycleFacade).executeTriggerDisable(
            eq(enabledWorkflowId), workflowExecutionIdArgumentCaptor.capture(), any(), any(), eq(CONNECTION_ID));
        verifyNoMoreInteractions(triggerLifecycleFacade);

        WorkflowExecutionId workflowExecutionId = workflowExecutionIdArgumentCaptor.getValue();

        assertThat(workflowExecutionId.getJobPrincipalId()).isEqualTo(integrationInstanceId);
        assertThat(workflowExecutionId.getWorkflowUuid()).isEqualTo(enabledWorkflowUuid);

        assertThat(integrationInstanceWorkflowCountsAtTriggerDisable).containsExactly(2);

        assertThat(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId))
            .isEmpty();
        assertThat(integrationInstanceService.getIntegrationInstances(List.of(integrationInstanceId))).isEmpty();
    }

    @Test
    void testDeleteOfADisabledInstanceLeavesItsTriggersAlone() {
        integrationInstanceService.updateEnabled(integrationInstanceId, false);

        connectedUserIntegrationFacade.deleteIntegrationInstance(ownerExternalUserId, integrationInstanceId);

        verifyNoInteractions(triggerLifecycleFacade);

        assertThat(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId))
            .isEmpty();
        assertThat(integrationInstanceService.getIntegrationInstances(List.of(integrationInstanceId))).isEmpty();
    }

    @Test
    void testDeleteDeniedForNonOwner() {
        assertThatThrownBy(
            () -> connectedUserIntegrationFacade.deleteIntegrationInstance(otherExternalUserId, integrationInstanceId))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(triggerLifecycleFacade);

        assertThat(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId))
            .extracting(IntegrationInstanceWorkflow::isEnabled)
            .containsExactlyInAnyOrder(true, false);
        assertThat(integrationInstanceService.getIntegrationInstances(List.of(integrationInstanceId)))
            .extracting(IntegrationInstance::getId)
            .containsExactly(integrationInstanceId);
    }

    private IntegrationWorkflow addWorkflow(long integrationId, String label) {
        Workflow workflow = workflowService.create(
            TRIGGER_WORKFLOW_DEFINITION.formatted(label), Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return integrationWorkflowService.addWorkflow(integrationId, 1, workflow.getId());
    }

    private void createIntegrationInstanceWorkflow(
        long integrationInstanceConfigurationId, String workflowId, boolean enabled) {

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        integrationInstanceConfigurationWorkflow = integrationInstanceConfigurationWorkflowService.create(
            integrationInstanceConfigurationWorkflow);

        IntegrationInstanceWorkflow integrationInstanceWorkflow =
            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstanceId, integrationInstanceConfigurationWorkflow.getId());

        integrationInstanceWorkflowService.updateEnabled(integrationInstanceWorkflow.getId(), enabled);
    }

    @Configuration
    @ComponentScan("com.bytechef.ee.embedded.connected.user.service")
    static class ConnectedUserServiceConfiguration {
    }

    private void exposeAsMcpWorkflow(
        long integrationId, long integrationInstanceConfigurationId, IntegrationWorkflow mcpIntegrationWorkflow) {

        IntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationId, mcpIntegrationWorkflow.getWorkflowId());

        when(mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurationsByIntegrationId(
            integrationId))
                .thenReturn(List.of(
                    new McpIntegrationInstanceConfiguration(
                        MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID, integrationInstanceConfigurationId,
                        MCP_SERVER_ID)));
        when(mcpServerService.getMcpServer(MCP_SERVER_ID))
            .thenReturn(new McpServer("embedded-server", PlatformType.EMBEDDED, Environment.PRODUCTION, true));
        when(mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID))
                    .thenReturn(List.of(
                        new McpIntegrationInstanceConfigurationWorkflow(
                            MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID,
                            mcpIntegrationInstanceConfigurationWorkflow.getId())));
    }

    @Nested
    class PermissionFilter {

        private static final String PRO_PLAN_PERMISSION_EXPRESSION = "metadata['plan'] == 'pro'";

        private static final String WORKFLOW_DEFINITION = """
            {"label":"%s","triggers":[],"tasks":[]}""";

        @Autowired
        private ComponentDefinitionService componentDefinitionService;

        @Autowired
        private ConnectionDefinitionService connectionDefinitionService;

        @Autowired
        private ConnectionService connectionService;

        @BeforeEach
        void beforeEach() {
            when(componentDefinitionService.getComponentDefinition(anyString(), any()))
                .thenReturn(new ComponentDefinition("slack"));
            when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
                .thenReturn(mock(ConnectionDefinition.class));
        }

        @Test
        void testNullExpressionIsVisible() {
            String externalUserId = createConnectedUser(Map.of());

            long integrationId = createIntegration(null);

            assertThat(getVisibleIntegrationIds(externalUserId)).contains(integrationId);
        }

        @Test
        void testMetadataMatchIsVisible() {
            String externalUserId = createConnectedUser(Map.of("plan", "pro"));

            long integrationId = createIntegration(PRO_PLAN_PERMISSION_EXPRESSION);

            assertThat(getVisibleIntegrationIds(externalUserId)).contains(integrationId);
        }

        @Test
        void testIntegrationVisibleWhenNoExpression() {
            String externalUserId = createConnectedUser(Map.of("plan", "free"));

            long integrationId = createIntegration(null);

            assertThat(getVisibleIntegrationIds(externalUserId)).contains(integrationId);
        }

        @Test
        void testIntegrationHiddenWhenExpressionMismatches() {
            String externalUserId = createConnectedUser(Map.of("plan", "free"));

            long hiddenIntegrationId = createIntegration(PRO_PLAN_PERMISSION_EXPRESSION);
            long visibleIntegrationId = createIntegration(null);

            assertThat(getVisibleIntegrationIds(externalUserId))
                .contains(visibleIntegrationId)
                .doesNotContain(hiddenIntegrationId);
        }

        @Test
        void testFilterWorkflowsDropsHiddenWorkflowsAndKeepsOthers() {
            String externalUserId = createConnectedUser(Map.of("plan", "free"));

            long integrationId = createIntegrationRow(null);
            long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integrationId, 1);

            String gatedWorkflowUuid = addWorkflow(integrationId, integrationInstanceConfigurationId, 1, "gated", true,
                PRO_PLAN_PERMISSION_EXPRESSION)
                    .getUuidAsString();
            String openWorkflowUuid =
                addWorkflow(integrationId, integrationInstanceConfigurationId, 1, "open", true, null)
                    .getUuidAsString();

            assertThat(getVisibleWorkflowUuids(externalUserId, integrationId))
                .containsExactly(openWorkflowUuid)
                .doesNotContain(gatedWorkflowUuid);
        }

        @Test
        void testFilterWorkflowsDropsDisabledAndMcpWorkflows() {
            String externalUserId = createConnectedUser(Map.of());

            long integrationId = createIntegrationRow(null);
            long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integrationId, 1);

            String visibleWorkflowUuid =
                addWorkflow(integrationId, integrationInstanceConfigurationId, 1, "enabled", true, null)
                    .getUuidAsString();

            addWorkflow(integrationId, integrationInstanceConfigurationId, 1, "disabled", false, null);

            IntegrationWorkflow mcpIntegrationWorkflow =
                addWorkflow(integrationId, integrationInstanceConfigurationId, 1, "mcp", true, null);

            exposeAsMcpWorkflow(integrationId, integrationInstanceConfigurationId, mcpIntegrationWorkflow);

            ConnectedUserIntegrationDTO connectedUserIntegrationDTO = getConnectedUserIntegration(
                externalUserId, integrationId);

            assertThat(connectedUserIntegrationDTO.mcpWorkflows())
                .extracting(ConnectedUserIntegrationDTO.McpWorkflowInfo::workflowUuid)
                .containsExactly(mcpIntegrationWorkflow.getUuidAsString());

            assertThat(getVisibleWorkflowUuids(externalUserId, integrationId)).containsExactly(visibleWorkflowUuid);
        }

        @Test
        void testIntegrationListsOnlyItsOwnInstancesWhenAnotherIntegrationSharesTheComponent() {
            String externalUserId = createConnectedUser(Map.of());

            long connectedIntegrationId = createIntegrationRow(null);
            long connectedIntegrationInstanceConfigurationId = createIntegrationInstanceConfiguration(
                connectedIntegrationId, 1);
            long otherIntegrationId = createIntegration(null);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                externalUserId, Environment.PRODUCTION);

            IntegrationInstance integrationInstance = integrationInstanceService.create(
                connectedUser.getId(), CONNECTION_ID, connectedIntegrationInstanceConfigurationId);

            Connection connection = mock(Connection.class);

            when(connection.getId()).thenReturn(CONNECTION_ID);
            when(connectionService.getConnections(anyList())).thenReturn(List.of(connection));

            assertThat(getConnectedUserIntegration(externalUserId, connectedIntegrationId).integrationInstances())
                .extracting(ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance::integrationInstance)
                .extracting(IntegrationInstance::getId)
                .containsExactly(integrationInstance.getId());
            assertThat(getConnectedUserIntegration(externalUserId, otherIntegrationId).integrationInstances())
                .isEmpty();
        }

        @Test
        void testPinnedInstanceCarriesItsOwnConfigurationWorkflows() {
            String externalUserId = createConnectedUser(Map.of());

            long integrationId = createIntegrationRow(null);
            long version1ConfigurationId = createIntegrationInstanceConfiguration(integrationId, 1);
            long version2ConfigurationId = createIntegrationInstanceConfiguration(integrationId, 2);

            IntegrationWorkflow version1Workflow = addWorkflow(
                integrationId, version1ConfigurationId, 1, "v1-only", true, null);
            IntegrationWorkflow version2Workflow = addWorkflow(
                integrationId, version2ConfigurationId, 2, "v2-only", true, null);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                externalUserId, Environment.PRODUCTION);

            IntegrationInstance pinnedInstance = integrationInstanceService.create(
                connectedUser.getId(), CONNECTION_ID, version1ConfigurationId);

            stubConnection();

            ConnectedUserIntegrationDTO integration = getConnectedUserIntegration(externalUserId, integrationId);

            assertThat(integration.integrationInstanceConfiguration()
                .integrationVersion()).isEqualTo(2);
            assertThat(integration.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .extracting(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                    .containsExactly(version2Workflow.getUuidAsString());

            ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance instance =
                integration.integrationInstances()
                    .getFirst();

            assertThat(instance.integrationInstance()
                .getId()).isEqualTo(pinnedInstance.getId());
            assertThat(instance.integrationInstanceConfiguration()
                .integrationVersion()).isEqualTo(1);
            assertThat(instance.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .extracting(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                    .containsExactly(version1Workflow.getUuidAsString());
        }

        @Test
        void testWorkflowToggleOnPinnedInstanceResolvesItsOwnVersion() {
            String externalUserId = createConnectedUser(Map.of());

            long integrationId = createIntegrationRow(null);
            long version1ConfigurationId = createIntegrationInstanceConfiguration(integrationId, 1);
            long version2ConfigurationId = createIntegrationInstanceConfiguration(integrationId, 2);

            IntegrationWorkflow version1Workflow = addWorkflow(
                integrationId, version1ConfigurationId, 1, "v1-only", true, null);

            addWorkflow(integrationId, version2ConfigurationId, 2, "v2-only", true, null);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                externalUserId, Environment.PRODUCTION);

            IntegrationInstance pinnedInstance = integrationInstanceService.create(
                connectedUser.getId(), CONNECTION_ID, version1ConfigurationId);

            assertThat(integrationWorkflowService.getWorkflowId(
                pinnedInstance.getId(), version1Workflow.getUuidAsString()))
                    .isEqualTo(version1Workflow.getWorkflowId());
        }

        @Test
        void testPinnedInstanceThroughSingleIntegrationReadCarriesItsOwnConfiguration() {
            String externalUserId = createConnectedUser(Map.of());

            stubSlackConnectionDefinition();

            long integrationId = createIntegrationRow(null);
            long version1ConfigurationId = createIntegrationInstanceConfiguration(
                integrationId, 1, AuthorizationType.BEARER_TOKEN);
            long version2ConfigurationId = createIntegrationInstanceConfiguration(
                integrationId, 2, AuthorizationType.API_KEY);

            IntegrationWorkflow version1Workflow = addWorkflow(
                integrationId, version1ConfigurationId, 1, "v1-only", true, null);
            IntegrationWorkflow version2Workflow = addWorkflow(
                integrationId, version2ConfigurationId, 2, "v2-only", true, null);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                externalUserId, Environment.PRODUCTION);

            IntegrationInstance pinnedInstance = integrationInstanceService.create(
                connectedUser.getId(), CONNECTION_ID, version1ConfigurationId);

            stubConnection();

            ConnectedUserIntegrationDTO integration = connectedUserIntegrationFacade.getConnectedUserIntegration(
                externalUserId, integrationId, true, Environment.PRODUCTION);

            assertThat(integration.integrationInstanceConfiguration()
                .integrationVersion()).isEqualTo(2);
            assertThat(integration.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .extracting(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                    .containsExactly(version2Workflow.getUuidAsString());
            assertThat(integration.connectionConfig()
                .authorizationType()).isEqualTo(AuthorizationType.API_KEY);

            ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance instance =
                integration.integrationInstances()
                    .getFirst();

            assertThat(instance.integrationInstance()
                .getId()).isEqualTo(pinnedInstance.getId());
            assertThat(instance.integrationInstanceConfiguration()
                .integrationVersion()).isEqualTo(1);
            assertThat(instance.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .extracting(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                    .containsExactly(version1Workflow.getUuidAsString());
            assertThat(instance.connectionConfig()
                .authorizationType()).isEqualTo(AuthorizationType.BEARER_TOKEN);
            assertThat(instance.connectionConfig()
                .inputs())
                    .extracting(ConnectedUserIntegrationDTO.Input::name)
                    .containsExactly("token");
        }

        private void stubSlackConnectionDefinition() {
            when(componentDefinitionService.getComponentDefinition(anyString(), any()))
                .thenReturn(
                    new ComponentDefinition(
                        ComponentDsl.component("slack")
                            .connection(
                                ComponentDsl.connection()
                                    .authorizations(
                                        ComponentDsl.authorization(AuthorizationType.BEARER_TOKEN)
                                            .properties(ComponentDsl.string("token")),
                                        ComponentDsl.authorization(AuthorizationType.API_KEY)
                                            .properties(ComponentDsl.string("key"))))));
        }

        @Nested
        @ContextConfiguration(classes = MethodSecurity.MethodSecurityConfiguration.class)
        @MockitoBean(types = PermissionService.class)
        class MethodSecurity {

            @AfterEach
            void afterEach() {
                SecurityContextHolder.clearContext();
            }

            @Test
            void testConnectedUserReadsPinnedInstanceConfigurationWithoutTenantAdmin() {
                String externalUserId = createConnectedUser(Map.of());

                long integrationId = createIntegrationRow(null);
                long version1ConfigurationId = createIntegrationInstanceConfiguration(integrationId, 1);

                createIntegrationInstanceConfiguration(integrationId, 2);

                ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                    externalUserId, Environment.PRODUCTION);

                integrationInstanceService.create(connectedUser.getId(), CONNECTION_ID, version1ConfigurationId);

                stubConnection();

                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new EmbeddedApiKeyAuthenticationToken(
                            Environment.PRODUCTION.ordinal(), connectedUser.getId(),
                            new User(externalUserId, "", List.of()), false));

                ConnectedUserIntegrationDTO integration = getConnectedUserIntegration(externalUserId, integrationId);

                assertThat(integration.integrationInstances()).hasSize(1);
                assertThat(integration.integrationInstances()
                    .getFirst()
                    .integrationInstanceConfiguration()
                    .integrationVersion()).isEqualTo(1);
            }

            @Configuration
            @EnableMethodSecurity
            static class MethodSecurityConfiguration {

                @Bean
                static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
                    @Lazy PermissionService permissionService) {

                    return new AutomationMethodSecurityExpressionHandler(permissionService);
                }
            }
        }

        private void stubConnection() {
            Connection connection = mock(Connection.class);

            when(connection.getId()).thenReturn(CONNECTION_ID);
            when(connectionService.getConnections(anyList())).thenReturn(List.of(connection));
        }

        private IntegrationWorkflow addWorkflow(
            long integrationId, long integrationInstanceConfigurationId, int integrationVersion, String label,
            boolean enabled, String permissionExpression) {

            Workflow workflow = workflowService.create(
                WORKFLOW_DEFINITION.formatted(label), Workflow.Format.JSON, Workflow.SourceType.JDBC);

            IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
                integrationId, integrationVersion, workflow.getId());

            if (permissionExpression != null) {
                integrationWorkflow = integrationWorkflowService.updatePermissionExpression(
                    integrationWorkflow.getId(), permissionExpression);
            }

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setEnabled(enabled);
            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfigurationId);
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflow.getId());

            integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

            return integrationWorkflow;
        }

        private String createConnectedUser(Map<String, Object> metadata) {
            String externalUserId = "filter-user-" + UUID.randomUUID();

            connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

            if (!metadata.isEmpty()) {
                connectedUserService.updateConnectedUser(externalUserId, Environment.PRODUCTION, metadata);
            }

            return externalUserId;
        }

        private long createIntegration(String permissionExpression) {
            long integrationId = createIntegrationRow(permissionExpression);

            createIntegrationInstanceConfiguration(integrationId, 1);

            return integrationId;
        }

        private long createIntegrationInstanceConfiguration(long integrationId, int integrationVersion) {
            return createIntegrationInstanceConfiguration(integrationId, integrationVersion, null);
        }

        private long createIntegrationInstanceConfiguration(
            long integrationId, int integrationVersion, AuthorizationType authorizationType) {

            IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

            if (authorizationType != null) {
                integrationInstanceConfiguration.setAuthorizationType(authorizationType);
            }

            integrationInstanceConfiguration.setConnectionParameters(Map.of());
            integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
            integrationInstanceConfiguration.setIntegrationId(integrationId);
            integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
            integrationInstanceConfiguration.setName("Slack");

            integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
                integrationInstanceConfiguration);

            integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), true);

            return integrationInstanceConfiguration.getId();
        }

        private long createIntegrationRow(String permissionExpression) {
            Integration integration = new Integration();

            integration.setComponentName("slack");
            integration.setName("Slack " + UUID.randomUUID());
            integration.setPermissionExpression(permissionExpression);

            integration = integrationService.create(integration);

            return integration.getId();
        }

        private ConnectedUserIntegrationDTO getConnectedUserIntegration(String externalUserId, long integrationId) {
            return connectedUserIntegrationFacade
                .getConnectedUserIntegrations(externalUserId, true, Environment.PRODUCTION)
                .stream()
                .filter(connectedUserIntegrationDTO -> Objects.equals(getIntegrationId(connectedUserIntegrationDTO),
                    integrationId))
                .findFirst()
                .orElseThrow();
        }

        private static Long getIntegrationId(ConnectedUserIntegrationDTO connectedUserIntegrationDTO) {
            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
                connectedUserIntegrationDTO.integrationInstanceConfiguration();

            return integrationInstanceConfigurationDTO.integrationId();
        }

        private List<Long> getVisibleIntegrationIds(String externalUserId) {
            return connectedUserIntegrationFacade
                .getConnectedUserIntegrations(externalUserId, true, Environment.PRODUCTION)
                .stream()
                .map(PermissionFilter::getIntegrationId)
                .toList();
        }

        private List<String> getVisibleWorkflowUuids(String externalUserId, long integrationId) {
            ConnectedUserIntegrationDTO connectedUserIntegrationDTO = getConnectedUserIntegration(
                externalUserId, integrationId);

            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
                connectedUserIntegrationDTO.integrationInstanceConfiguration();

            return integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows()
                .stream()
                .map(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                .toList();
        }
    }

    @Nested
    class ConnectionName {

        @Autowired
        private ComponentDefinitionService componentDefinitionService;

        @Autowired
        private ConnectionFacade connectionFacade;

        @Autowired
        private ConnectionService connectionService;

        private final Map<Long, String> connectionNamesById = new HashMap<>();

        private long nextConnectionId = 1000L;

        @BeforeEach
        void beforeEach() {
            ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

            when(componentDefinition.getConnection()).thenReturn(mock(ConnectionDefinition.class));
            when(componentDefinitionService.getComponentDefinition(anyString(), any()))
                .thenReturn(componentDefinition);

            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenAnswer(invocation -> {
                    ConnectionDTO connectionDTO = invocation.getArgument(0);

                    long connectionId = nextConnectionId++;

                    connectionNamesById.put(connectionId, connectionDTO.name());

                    return connectionId;
                });

            when(connectionService.getConnections(anyList()))
                .thenAnswer(invocation -> {
                    List<Long> connectionIds = invocation.getArgument(0);

                    return connectionIds.stream()
                        .filter(connectionNamesById::containsKey)
                        .map(connectionId -> createConnection(connectionId, connectionNamesById.get(connectionId)))
                        .toList();
                });
        }

        @Test
        void testFirstConnectionKeepsTheConfigurationName() {
            String externalUserId = createConnectedUser();

            assertThat(connect(externalUserId)).isEqualTo("Slack");
        }

        @Test
        void testSecondConnectionGetsTheNextNumber() {
            String externalUserId = createConnectedUser();

            assertThat(connect(externalUserId)).isEqualTo("Slack");
            assertThat(connect(externalUserId)).isEqualTo("Slack 2");
        }

        @Test
        void testNextConnectionTakesTheLowestFreeNumber() {
            String externalUserId = createConnectedUser();

            assertThat(connect(externalUserId)).isEqualTo("Slack");
            assertThat(connect(externalUserId)).isEqualTo("Slack 2");
            assertThat(connect(externalUserId)).isEqualTo("Slack 3");

            deleteConnection(externalUserId, "Slack 2");

            assertThat(connect(externalUserId)).isEqualTo("Slack 2");
        }

        @Test
        void testAnotherConnectedUsersConnectionsDoNotAffectTheNumbering() {
            String otherExternalUserId = createConnectedUser();

            connect(otherExternalUserId);
            connect(otherExternalUserId);

            String externalUserId = createConnectedUser();

            assertThat(connect(externalUserId)).isEqualTo("Slack");
            assertThat(connect(externalUserId)).isEqualTo("Slack 2");
        }

        private String connect(String externalUserId) {
            IntegrationInstance integrationInstance = connectedUserIntegrationFacade.createIntegrationInstance(
                externalUserId, integrationId, Map.of(), Environment.PRODUCTION);

            return connectionNamesById.get(integrationInstance.getConnectionId());
        }

        private Connection createConnection(long connectionId, String name) {
            Connection connection = new Connection();

            connection.setId(connectionId);
            connection.setName(name);

            return connection;
        }

        private String createConnectedUser() {
            String externalUserId = "connection-name-user-" + UUID.randomUUID();

            connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

            return externalUserId;
        }

        private void deleteConnection(String externalUserId, String name) {
            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                externalUserId, Environment.PRODUCTION);

            IntegrationInstance integrationInstance = integrationInstanceService
                .getIntegrationInstances(connectedUser.getId(), "slack", Environment.PRODUCTION)
                .stream()
                .filter(curIntegrationInstance -> Objects.equals(
                    connectionNamesById.get(curIntegrationInstance.getConnectionId()), name))
                .findFirst()
                .orElseThrow();

            integrationInstanceService.delete(integrationInstance.getId());

            connectionNamesById.remove(integrationInstance.getConnectionId());
        }
    }

    @Nested
    class ProvisioningTests {

        private static final String PRO_PLAN_PERMISSION_EXPRESSION = "metadata['plan'] == 'pro'";

        @Autowired
        private ComponentDefinitionService componentDefinitionService;

        @Autowired
        private ConnectionDefinitionService connectionDefinitionService;

        @Autowired
        private ConnectionFacade connectionFacade;

        @Autowired
        private ConnectionService connectionService;

        @Autowired
        private OAuth2ParametersFacade oAuth2ParametersFacade;

        @Autowired
        private OAuth2Service oAuth2Service;

        private long connectedUserId;
        private long provisioningIntegrationId;
        private long version1ConfigurationId;
        private String workflowIdA;
        private String workflowIdB;
        private String workflowUuidA;
        private String workflowUuidB;

        @BeforeEach
        void beforeEach() {
            when(componentDefinitionService.getComponentDefinition(anyString(), any()))
                .thenReturn(
                    new ComponentDefinition(
                        ComponentDsl.component("slack")
                            .connection(
                                ComponentDsl.connection()
                                    .authorizations(
                                        ComponentDsl.authorization(AuthorizationType.API_KEY)
                                            .properties(ComponentDsl.string("token"))))));
            when(connectionDefinitionService.getConnectionDefinition(anyString(), any()))
                .thenReturn(mock(ConnectionDefinition.class));
            when(connectionFacade.create(any(ConnectionDTO.class), eq(PlatformType.EMBEDDED)))
                .thenReturn(CONNECTION_ID);

            connectedUserId = createConnectedUser(Map.of());
            provisioningIntegrationId = createIntegrationRow(null);
            version1ConfigurationId = createConfiguration(
                provisioningIntegrationId, 1, Environment.PRODUCTION, true, Map.of("sharedKey", "shared"));

            IntegrationWorkflow integrationWorkflowA = addConfigurationWorkflow(
                provisioningIntegrationId, version1ConfigurationId, 1, "workflow-a", null);

            workflowIdA = integrationWorkflowA.getWorkflowId();
            workflowUuidA = integrationWorkflowA.getUuidAsString();

            IntegrationWorkflow integrationWorkflowB = addConfigurationWorkflow(
                provisioningIntegrationId, version1ConfigurationId, 1, "workflow-b", null);

            workflowIdB = integrationWorkflowB.getWorkflowId();
            workflowUuidB = integrationWorkflowB.getUuidAsString();
        }

        @Test
        void testProvisioningCreatesConnectionInstanceAndEnabledWorkflowsWithInputs() {
            long instanceId = connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of("token", "t"),
                List.of(new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidA, Map.of("channel", "#ops"))));

            IntegrationInstance instance = integrationInstanceService.getIntegrationInstance(instanceId);

            assertThat(instance.getIntegrationInstanceConfigurationId()).isEqualTo(version1ConfigurationId);
            assertThat(instance.getConnectionId()).isEqualTo(CONNECTION_ID);
            assertThat(instance.getConnectedUserId()).isEqualTo(connectedUserId);

            IntegrationInstanceWorkflow workflowA = integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(instanceId, workflowIdA)
                .orElseThrow();

            Map<String, ?> workflowAInputs = workflowA.getInputs();

            assertThat(workflowA.isEnabled()).isTrue();
            assertThat(workflowAInputs.get("channel")).isEqualTo("#ops");
            assertThat(integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(instanceId, workflowIdB))
                .isEmpty();

            ArgumentCaptor<ConnectionDTO> connectionCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

            verify(connectionFacade).create(connectionCaptor.capture(), eq(PlatformType.EMBEDDED));

            ConnectionDTO connectionDTO = connectionCaptor.getValue();

            Map<String, ?> connectionParameters = connectionDTO.parameters();

            assertThat(connectionParameters.get("token")).isEqualTo("t");
            assertThat(connectionParameters.get("sharedKey")).isEqualTo("shared");
            assertThat(connectionDTO.authorizationType()).isEqualTo(AuthorizationType.API_KEY);

            verify(triggerLifecycleFacade).executeTriggerEnable(
                eq(workflowIdA), any(), any(), any(), eq(CONNECTION_ID), any(), anyLong());
            verify(triggerLifecycleFacade, never()).executeTriggerEnable(
                eq(workflowIdB), any(), any(), any(), any(), any(), anyLong());
        }

        @Test
        void testProvisionedConnectionIsOwnedByTheConnectedUserLogin() {
            connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of("token", "t"), List.of());

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

            verify(connectionService).updateCreatedBy(CONNECTION_ID, connectedUser.getExternalId());
        }

        @Test
        void testProvisioningTreatsNullConnectionParametersAndWorkflowsAsEmpty() {
            long instanceId = connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, null, null);

            assertThat(integrationInstanceService.getIntegrationInstance(instanceId)
                .getIntegrationInstanceConfigurationId()).isEqualTo(version1ConfigurationId);

            ArgumentCaptor<ConnectionDTO> connectionCaptor = ArgumentCaptor.forClass(ConnectionDTO.class);

            verify(connectionFacade).create(connectionCaptor.capture(), eq(PlatformType.EMBEDDED));

            ConnectionDTO connectionDTO = connectionCaptor.getValue();

            assertThat(connectionDTO.parameters()).containsOnlyKeys("sharedKey");
        }

        @Test
        void testProvisioningRefusesAnMcpWorkflow() {
            IntegrationWorkflow mcpIntegrationWorkflow = addConfigurationWorkflow(
                provisioningIntegrationId, version1ConfigurationId, 1, "mcp", null);

            exposeAsMcpWorkflow(provisioningIntegrationId, version1ConfigurationId, mcpIntegrationWorkflow);

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(),
                List.of(
                    new ProvisionIntegrationInstanceWorkflowDTO(mcpIntegrationWorkflow.getUuidAsString(), Map.of()))))
                        .isInstanceOf(ConfigurationException.class)
                        .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                        .isEqualTo(
                            IntegrationInstanceConfigurationErrorType.WORKFLOW_NOT_IN_CONFIGURATION.getErrorKey());

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningRefusesADisabledConfiguration() {
            long disabledConfigurationId = createConfiguration(
                provisioningIntegrationId, 2, Environment.PRODUCTION, false, Map.of());

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, disabledConfigurationId, Map.of(), List.of()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                    .isEqualTo(IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_DISABLED.getErrorKey());

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningRefusesAConfigurationFromAnotherEnvironment() {
            long stagingConfigurationId = createConfiguration(
                provisioningIntegrationId, 1, Environment.STAGING, true, Map.of());

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, stagingConfigurationId, Map.of(), List.of()))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                    .isEqualTo(
                        IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_NOT_FOUND.getErrorKey());

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningRefusesAWorkflowNotInTheConfiguration() {
            long version2ConfigurationId = createConfiguration(
                provisioningIntegrationId, 2, Environment.PRODUCTION, true, Map.of());

            IntegrationWorkflow version2OnlyWorkflow = addConfigurationWorkflow(
                provisioningIntegrationId, version2ConfigurationId, 2, "v2-only", null);

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(),
                List.of(
                    new ProvisionIntegrationInstanceWorkflowDTO(version2OnlyWorkflow.getUuidAsString(), Map.of()))))
                        .isInstanceOf(ConfigurationException.class)
                        .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                        .isEqualTo(
                            IntegrationInstanceConfigurationErrorType.WORKFLOW_NOT_IN_CONFIGURATION.getErrorKey());

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningRefusesADisabledConfigurationWorkflow() {
            IntegrationWorkflow disabledWorkflow = addConfigurationWorkflow(
                provisioningIntegrationId, version1ConfigurationId, 1, "disabled", null, false);

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(),
                List.of(new ProvisionIntegrationInstanceWorkflowDTO(disabledWorkflow.getUuidAsString(), Map.of()))))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
                    .isEqualTo(IntegrationInstanceConfigurationErrorType.WORKFLOW_NOT_IN_CONFIGURATION.getErrorKey());

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningRefusesAnIntegrationHiddenByThePermissionExpression() {
            long hiddenIntegrationId = createIntegrationRow(PRO_PLAN_PERMISSION_EXPRESSION);
            long hiddenConfigurationId = createConfiguration(
                hiddenIntegrationId, 1, Environment.PRODUCTION, true, Map.of());

            long freeConnectedUserId = createConnectedUser(Map.of("plan", "free"));

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                freeConnectedUserId, hiddenConfigurationId, Map.of(), List.of()))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(freeConnectedUserId))
                .isEmpty();

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningRefusesAWorkflowHiddenByThePermissionExpression() {
            IntegrationWorkflow gatedWorkflow = addConfigurationWorkflow(
                provisioningIntegrationId, version1ConfigurationId, 1, "gated", PRO_PLAN_PERMISSION_EXPRESSION);

            long freeConnectedUserId = createConnectedUser(Map.of("plan", "free"));

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                freeConnectedUserId, version1ConfigurationId, Map.of(),
                List.of(new ProvisionIntegrationInstanceWorkflowDTO(gatedWorkflow.getUuidAsString(), Map.of()))))
                    .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(freeConnectedUserId))
                .isEmpty();

            verifyNoInteractions(connectionFacade);
        }

        @Test
        void testProvisioningAllowsASecondInstanceOnTheSameConfiguration() {
            long firstInstanceId = connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(), List.of());
            long secondInstanceId = connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(), List.of());

            assertThat(secondInstanceId).isNotEqualTo(firstInstanceId);
            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId))
                .extracting(IntegrationInstance::getId, IntegrationInstance::getIntegrationInstanceConfigurationId)
                .containsExactlyInAnyOrder(
                    tuple(firstInstanceId, version1ConfigurationId), tuple(secondInstanceId, version1ConfigurationId));
        }

        @Test
        void testProvisioningRollsBackInstanceConnectionAndTriggersWhenAWorkflowFailsToEnable() {
            doThrow(new IllegalStateException("trigger registration failed"))
                .when(triggerLifecycleFacade)
                .executeTriggerEnable(eq(workflowIdB), any(), any(), any(), any(), any(), anyLong());

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(),
                List.of(
                    new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidA, Map.of()),
                    new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidB, Map.of()))))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("trigger registration failed");

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();

            verify(triggerLifecycleFacade).executeTriggerDisable(
                eq(workflowIdA), any(), any(), any(), eq(CONNECTION_ID));
            verify(connectionFacade).delete(CONNECTION_ID);
        }

        @Test
        void testProvisioningDisablesTheWorkflowWhoseEnableFailedAfterItsTriggerWasRegistered() {
            IllegalStateException enableFailure = new IllegalStateException("trigger state save failed");

            doThrow(enableFailure)
                .when(triggerLifecycleFacade)
                .executeTriggerEnable(eq(workflowIdB), any(), any(), any(), any(), any(), anyLong());

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(),
                List.of(
                    new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidA, Map.of()),
                    new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidB, Map.of()))))
                        .isSameAs(enableFailure);

            verify(triggerLifecycleFacade).executeTriggerDisable(
                eq(workflowIdB), any(), any(), any(), eq(CONNECTION_ID));
            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();
            verify(connectionFacade).delete(CONNECTION_ID);
        }

        @Test
        void testProvisioningKeepsTheEnableFailureWhenDisablingTheInFlightWorkflowFails() {
            IllegalStateException enableFailure = new IllegalStateException("trigger state save failed");
            IllegalStateException disableFailure = new IllegalStateException("trigger disable failed");

            doThrow(enableFailure)
                .when(triggerLifecycleFacade)
                .executeTriggerEnable(eq(workflowIdA), any(), any(), any(), any(), any(), anyLong());
            doThrow(disableFailure)
                .when(triggerLifecycleFacade)
                .executeTriggerDisable(eq(workflowIdA), any(), any(), any(), any());

            assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                connectedUserId, version1ConfigurationId, Map.of(),
                List.of(new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidA, Map.of()))))
                    .isSameAs(enableFailure)
                    .hasSuppressedException(disableFailure);

            assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();
            verify(connectionFacade).delete(CONNECTION_ID);
        }

        @Test
        void testProvisionableIntegrationsListsEnabledVisibleConfigurationsAndMarksTheSelfServeOne() {
            long listedIntegrationId = createIntegrationRow(null, 3);

            long listedVersion1ConfigurationId = createConfiguration(
                listedIntegrationId, 1, Environment.PRODUCTION, true, Map.of("clientId", "x"));
            long listedVersion2ConfigurationId = createConfiguration(
                listedIntegrationId, 2, Environment.PRODUCTION, true, Map.of());

            createConfiguration(listedIntegrationId, 3, Environment.PRODUCTION, false, Map.of());

            String visibleWorkflowUuid = addConfigurationWorkflow(
                listedIntegrationId, listedVersion1ConfigurationId, 1, "visible", null)
                    .getUuidAsString();

            addConfigurationWorkflow(
                listedIntegrationId, listedVersion1ConfigurationId, 1, "gated", PRO_PLAN_PERMISSION_EXPRESSION);
            addConfigurationWorkflow(listedIntegrationId, listedVersion1ConfigurationId, 1, "disabled", null, false);
            addConfigurationWorkflow(listedIntegrationId, listedVersion2ConfigurationId, 2, "v2", null);

            long hiddenIntegrationId = createIntegrationRow(PRO_PLAN_PERMISSION_EXPRESSION);

            createConfiguration(hiddenIntegrationId, 1, Environment.PRODUCTION, true, Map.of());

            long stagingIntegrationId = createIntegrationRow(null);

            createConfiguration(stagingIntegrationId, 1, Environment.STAGING, true, Map.of());

            long freeConnectedUserId = createConnectedUser(Map.of("plan", "free"));

            List<ProvisionableIntegrationDTO> integrations =
                connectedUserIntegrationFacade.getProvisionableIntegrations(freeConnectedUserId);

            assertThat(integrations)
                .extracting(integration -> integration.integration()
                    .id())
                .contains(listedIntegrationId)
                .doesNotContain(hiddenIntegrationId, stagingIntegrationId);

            ProvisionableIntegrationDTO listedIntegration = integrations.stream()
                .filter(integration -> Objects.equals(
                    integration.integration()
                        .id(),
                    listedIntegrationId))
                .findFirst()
                .orElseThrow();

            assertThat(listedIntegration.componentVersion()).isEqualTo(3);

            List<ProvisionableIntegrationInstanceConfiguration> configurations =
                listedIntegration.integrationInstanceConfigurations();

            assertThat(configurations)
                .extracting(
                    configuration -> configuration.integrationInstanceConfiguration()
                        .integrationVersion(),
                    ProvisionableIntegrationInstanceConfiguration::selfServe)
                .containsExactly(tuple(1, false), tuple(2, true));

            ProvisionableIntegrationInstanceConfiguration version1Configuration = configurations.getFirst();

            assertThat(version1Configuration.sharedConnectionParameterNames()).containsExactly("clientId");
            assertThat(version1Configuration.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .extracting(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                    .containsExactly(visibleWorkflowUuid);
            assertThat(version1Configuration.oAuth2AuthorizationParameters()).isNull();
        }

        @Test
        void testProvisionableIntegrationsUseThePermissionExpressionOfTheConfigurationsOwnVersion() {
            long versionedIntegrationId = createIntegrationRow(null);

            long versionedVersion1ConfigurationId = createConfiguration(
                versionedIntegrationId, 1, Environment.PRODUCTION, true, Map.of());
            long versionedVersion2ConfigurationId = createConfiguration(
                versionedIntegrationId, 2, Environment.PRODUCTION, true, Map.of());

            IntegrationWorkflow version1Workflow = addConfigurationWorkflow(
                versionedIntegrationId, versionedVersion1ConfigurationId, 1, "versioned", null);

            IntegrationWorkflow version2Workflow = addConfigurationWorkflow(
                versionedIntegrationId, versionedVersion2ConfigurationId, 2, "versioned",
                PRO_PLAN_PERMISSION_EXPRESSION);

            version2Workflow.setUuid(version1Workflow.getUuidAsString());

            integrationWorkflowService.update(version2Workflow);

            long freeConnectedUserId = createConnectedUser(Map.of("plan", "free"));

            ProvisionableIntegrationDTO versionedIntegration = connectedUserIntegrationFacade
                .getProvisionableIntegrations(freeConnectedUserId)
                .stream()
                .filter(integration -> Objects.equals(
                    integration.integration()
                        .id(),
                    versionedIntegrationId))
                .findFirst()
                .orElseThrow();

            List<ProvisionableIntegrationInstanceConfiguration> configurations =
                versionedIntegration.integrationInstanceConfigurations();

            assertThat(configurations.getFirst()
                .integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .extracting(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
                    .containsExactly(version1Workflow.getUuidAsString());
            assertThat(configurations.getLast()
                .integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows())
                    .isEmpty();
        }

        @Test
        void testProvisionableIntegrationsCarryOAuth2ParametersAndRedirectUriOfAnOAuth2Configuration() {
            long oAuth2IntegrationId = createIntegrationRow(null);

            createConfiguration(
                oAuth2IntegrationId, 1, Environment.PRODUCTION, true, Map.of("clientId", "client"),
                AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            OAuth2AuthorizationParameters oAuth2AuthorizationParameters = new OAuth2AuthorizationParameters(
                "https://slack.com/oauth", "client", Map.of(), Map.of());

            when(oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
                eq("slack"), anyInt(), any(), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)))
                    .thenReturn(oAuth2AuthorizationParameters);
            when(oAuth2Service.getRedirectUri()).thenReturn("https://bytechef/callback");

            ProvisionableIntegrationDTO oAuth2Integration = connectedUserIntegrationFacade
                .getProvisionableIntegrations(connectedUserId)
                .stream()
                .filter(integration -> Objects.equals(
                    integration.integration()
                        .id(),
                    oAuth2IntegrationId))
                .findFirst()
                .orElseThrow();

            List<ProvisionableIntegrationInstanceConfiguration> configurations =
                oAuth2Integration.integrationInstanceConfigurations();

            ProvisionableIntegrationInstanceConfiguration configuration = configurations.getFirst();

            assertThat(configuration.oAuth2AuthorizationParameters()).isEqualTo(oAuth2AuthorizationParameters);
            assertThat(configuration.redirectUri()).isEqualTo("https://bytechef/callback");
            assertThat(configuration.sharedConnectionParameterNames()).containsExactly("clientId");
        }

        @Test
        void testProvisionableIntegrationsSkipAnIntegrationWhoseComponentDefinitionFailsToLoad() {
            long brokenIntegrationId = createIntegrationRow(null, 5);

            createConfiguration(brokenIntegrationId, 1, Environment.PRODUCTION, true, Map.of());

            when(componentDefinitionService.getComponentDefinition("slack", 5))
                .thenThrow(new IllegalStateException("component definition slack/v5 not found"));

            assertThat(connectedUserIntegrationFacade.getProvisionableIntegrations(connectedUserId))
                .extracting(integration -> integration.integration()
                    .id())
                .contains(provisioningIntegrationId)
                .doesNotContain(brokenIntegrationId);
        }

        @Test
        void testProvisionableIntegrationsSkipAConfigurationThatFailsToAssembleAndKeepTheSelfServeFlag() {
            long partlyBrokenIntegrationId = createIntegrationRow(null);

            long version1ConfigurationId = createConfiguration(
                partlyBrokenIntegrationId, 1, Environment.PRODUCTION, true, Map.of());

            createConfiguration(
                partlyBrokenIntegrationId, 2, Environment.PRODUCTION, true, Map.of("clientId", "client"),
                AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            when(oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
                eq("slack"), anyInt(), any(), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)))
                    .thenThrow(new IllegalStateException("OAuth2 parameters unavailable"));

            ProvisionableIntegrationDTO partlyBrokenIntegration = connectedUserIntegrationFacade
                .getProvisionableIntegrations(connectedUserId)
                .stream()
                .filter(integration -> Objects.equals(
                    integration.integration()
                        .id(),
                    partlyBrokenIntegrationId))
                .findFirst()
                .orElseThrow();

            assertThat(partlyBrokenIntegration.integrationInstanceConfigurations())
                .extracting(
                    configuration -> configuration.integrationInstanceConfiguration()
                        .id(),
                    ProvisionableIntegrationInstanceConfiguration::selfServe)
                .containsExactly(tuple(version1ConfigurationId, false));
        }

        @Test
        void testProvisionableIntegrationsSkipAConfigurationWhoseWorkflowFailsToLoadInATransactionalService() {
            long partlyBrokenIntegrationId = createIntegrationRow(null);

            long version1ConfigurationId = createConfiguration(
                partlyBrokenIntegrationId, 1, Environment.PRODUCTION, true, Map.of());
            long version2ConfigurationId = createConfiguration(
                partlyBrokenIntegrationId, 2, Environment.PRODUCTION, true, Map.of());

            String missingWorkflowId = "missing-" + UUID.randomUUID();

            integrationWorkflowService.addWorkflow(partlyBrokenIntegrationId, 2, missingWorkflowId);

            IntegrationInstanceConfigurationWorkflow brokenConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            brokenConfigurationWorkflow.setEnabled(true);
            brokenConfigurationWorkflow.setIntegrationInstanceConfigurationId(version2ConfigurationId);
            brokenConfigurationWorkflow.setWorkflowId(missingWorkflowId);

            integrationInstanceConfigurationWorkflowService.create(brokenConfigurationWorkflow);

            ProvisionableIntegrationDTO partlyBrokenIntegration = connectedUserIntegrationFacade
                .getProvisionableIntegrations(connectedUserId)
                .stream()
                .filter(integration -> Objects.equals(
                    integration.integration()
                        .id(),
                    partlyBrokenIntegrationId))
                .findFirst()
                .orElseThrow();

            assertThat(partlyBrokenIntegration.integrationInstanceConfigurations())
                .extracting(
                    configuration -> configuration.integrationInstanceConfiguration()
                        .id())
                .containsExactly(version1ConfigurationId);
        }

        @Nested
        @ContextConfiguration(classes = ProvisioningMethodSecurity.MethodSecurityConfiguration.class)
        @MockitoBean(types = PermissionService.class)
        class ProvisioningMethodSecurity {

            @Autowired
            private IntegrationInstanceAdminFacade integrationInstanceAdminFacade;

            @Autowired
            private PermissionService permissionService;

            @AfterEach
            void afterEach() {
                SecurityContextHolder.clearContext();
            }

            @Test
            void testTenantAdminProvisionsThroughTheGuardedChain() {
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

                when(permissionService.isTenantAdmin()).thenReturn(true);

                assertThat(integrationInstanceAdminFacade.getProvisionableIntegrations(connectedUserId))
                    .extracting(integration -> integration.integration()
                        .id())
                    .contains(provisioningIntegrationId);

                long instanceId = integrationInstanceAdminFacade.provisionIntegrationInstance(
                    connectedUserId, version1ConfigurationId, Map.of("token", "t"),
                    List.of(new ProvisionIntegrationInstanceWorkflowDTO(workflowUuidA, Map.of("channel", "#ops"))));

                IntegrationInstanceWorkflow workflowA = integrationInstanceWorkflowService
                    .fetchIntegrationInstanceWorkflow(instanceId, workflowIdA)
                    .orElseThrow();

                assertThat(workflowA.isEnabled()).isTrue();
                assertThat(integrationInstanceService.getIntegrationInstance(instanceId)
                    .getConnectedUserId()).isEqualTo(connectedUserId);
            }

            @Test
            void testConnectedUserIsDeniedOnTheAdminAndSharedFacades() {
                ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new EmbeddedApiKeyAuthenticationToken(
                            Environment.PRODUCTION.ordinal(), connectedUserId,
                            new User(connectedUser.getExternalId(), "", List.of()), false));

                assertThatThrownBy(() -> integrationInstanceAdminFacade.provisionIntegrationInstance(
                    connectedUserId, version1ConfigurationId, Map.of(), List.of()))
                        .isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> integrationInstanceAdminFacade.getProvisionableIntegrations(connectedUserId))
                    .isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> connectedUserIntegrationFacade.provisionIntegrationInstance(
                    connectedUserId, version1ConfigurationId, Map.of(), List.of()))
                        .isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> connectedUserIntegrationFacade.getProvisionableIntegrations(connectedUserId))
                    .isInstanceOf(AccessDeniedException.class);

                assertThat(integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId)).isEmpty();

                verifyNoInteractions(connectionFacade);
            }

            @Configuration
            @EnableMethodSecurity
            static class MethodSecurityConfiguration {

                @Bean
                static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
                    @Lazy PermissionService permissionService) {

                    return new AutomationMethodSecurityExpressionHandler(permissionService);
                }
            }
        }

        private IntegrationWorkflow addConfigurationWorkflow(
            long integrationId, long integrationInstanceConfigurationId, int integrationVersion, String label,
            String permissionExpression) {

            return addConfigurationWorkflow(
                integrationId, integrationInstanceConfigurationId, integrationVersion, label, permissionExpression,
                true);
        }

        private IntegrationWorkflow addConfigurationWorkflow(
            long integrationId, long integrationInstanceConfigurationId, int integrationVersion, String label,
            String permissionExpression, boolean enabled) {

            Workflow workflow = workflowService.create(
                TRIGGER_WORKFLOW_DEFINITION.formatted(label), Workflow.Format.JSON, Workflow.SourceType.JDBC);

            IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
                integrationId, integrationVersion, workflow.getId());

            if (permissionExpression != null) {
                integrationWorkflow = integrationWorkflowService.updatePermissionExpression(
                    integrationWorkflow.getId(), permissionExpression);
            }

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setEnabled(enabled);
            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfigurationId);
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflow.getId());

            integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

            return integrationWorkflow;
        }

        private long createConfiguration(
            long integrationId, int integrationVersion, Environment environment, boolean enabled,
            Map<String, Object> connectionParameters) {

            return createConfiguration(
                integrationId, integrationVersion, environment, enabled, connectionParameters,
                AuthorizationType.API_KEY);
        }

        private long createConfiguration(
            long integrationId, int integrationVersion, Environment environment, boolean enabled,
            Map<String, Object> connectionParameters, AuthorizationType authorizationType) {

            IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

            integrationInstanceConfiguration.setAuthorizationType(authorizationType);
            integrationInstanceConfiguration.setConnectionParameters(connectionParameters);
            integrationInstanceConfiguration.setEnvironment(environment);
            integrationInstanceConfiguration.setIntegrationId(integrationId);
            integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
            integrationInstanceConfiguration.setName("Slack");

            integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
                integrationInstanceConfiguration);

            integrationInstanceConfigurationService.updateEnabled(integrationInstanceConfiguration.getId(), enabled);

            return integrationInstanceConfiguration.getId();
        }

        private long createConnectedUser(Map<String, Object> metadata) {
            String externalUserId = "provisioning-user-" + UUID.randomUUID();

            ConnectedUser connectedUser = connectedUserService.createConnectedUser(
                externalUserId, Environment.PRODUCTION);

            if (!metadata.isEmpty()) {
                connectedUserService.updateConnectedUser(externalUserId, Environment.PRODUCTION, metadata);
            }

            return connectedUser.getId();
        }

        private long createIntegrationRow(String permissionExpression) {
            return createIntegrationRow(permissionExpression, 1);
        }

        private long createIntegrationRow(String permissionExpression, int componentVersion) {
            Integration integration = new Integration();

            integration.setComponentName("slack");
            integration.setComponentVersion(componentVersion);
            integration.setName("Slack " + UUID.randomUUID());
            integration.setPermissionExpression(permissionExpression);

            integration = integrationService.create(integration);

            return integration.getId();
        }
    }
}

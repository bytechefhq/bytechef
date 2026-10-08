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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
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
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private WorkflowService workflowService;

    private String disabledWorkflowId;
    private String enabledWorkflowId;
    private String enabledWorkflowUuid;
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

    @Nested
    class PermissionFilter {

        private static final long MCP_INTEGRATION_INSTANCE_CONFIGURATION_ID = 501L;
        private static final long MCP_SERVER_ID = 601L;
        private static final String PRO_PLAN_PERMISSION_EXPRESSION = "metadata['plan'] == 'pro'";

        private static final String WORKFLOW_DEFINITION = """
            {"label":"%s","triggers":[],"tasks":[]}""";

        @Autowired
        private ComponentDefinitionService componentDefinitionService;

        @Autowired
        private ConnectionDefinitionService connectionDefinitionService;

        @Autowired
        private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

        @Autowired
        private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

        @Autowired
        private McpServerService mcpServerService;

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
            long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integrationId);

            String gatedWorkflowUuid = addWorkflow(
                integrationId, integrationInstanceConfigurationId, "gated", true, PRO_PLAN_PERMISSION_EXPRESSION)
                    .getUuidAsString();
            String openWorkflowUuid = addWorkflow(integrationId, integrationInstanceConfigurationId, "open", true, null)
                .getUuidAsString();

            assertThat(getVisibleWorkflowUuids(externalUserId, integrationId))
                .containsExactly(openWorkflowUuid)
                .doesNotContain(gatedWorkflowUuid);
        }

        @Test
        void testFilterWorkflowsDropsDisabledAndMcpWorkflows() {
            String externalUserId = createConnectedUser(Map.of());

            long integrationId = createIntegrationRow(null);
            long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(integrationId);

            String visibleWorkflowUuid = addWorkflow(
                integrationId, integrationInstanceConfigurationId, "enabled", true, null)
                    .getUuidAsString();

            addWorkflow(integrationId, integrationInstanceConfigurationId, "disabled", false, null);

            IntegrationWorkflow mcpIntegrationWorkflow = addWorkflow(
                integrationId, integrationInstanceConfigurationId, "mcp", true, null);

            exposeAsMcpWorkflow(integrationId, integrationInstanceConfigurationId, mcpIntegrationWorkflow);

            ConnectedUserIntegrationDTO connectedUserIntegrationDTO = getConnectedUserIntegration(
                externalUserId, integrationId);

            assertThat(connectedUserIntegrationDTO.mcpWorkflows())
                .extracting(ConnectedUserIntegrationDTO.McpWorkflowInfo::workflowUuid)
                .containsExactly(mcpIntegrationWorkflow.getUuidAsString());

            assertThat(getVisibleWorkflowUuids(externalUserId, integrationId)).containsExactly(visibleWorkflowUuid);
        }

        private IntegrationWorkflow addWorkflow(
            long integrationId, long integrationInstanceConfigurationId, String label, boolean enabled,
            String permissionExpression) {

            Workflow workflow = workflowService.create(
                WORKFLOW_DEFINITION.formatted(label), Workflow.Format.JSON, Workflow.SourceType.JDBC);

            IntegrationWorkflow integrationWorkflow = integrationWorkflowService.addWorkflow(
                integrationId, 1, workflow.getId());

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

            createIntegrationInstanceConfiguration(integrationId);

            return integrationId;
        }

        private long createIntegrationInstanceConfiguration(long integrationId) {
            IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

            integrationInstanceConfiguration.setConnectionParameters(Map.of());
            integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
            integrationInstanceConfiguration.setIntegrationId(integrationId);
            integrationInstanceConfiguration.setIntegrationVersion(1);
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
}

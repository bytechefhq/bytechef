/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO.WorkflowConnectionDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class McpIntegrationInstanceConfigurationFacadeTest {

    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 42L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private EmbeddedMcpReader embeddedMcpReader;

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    @Mock
    private IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @Mock
    private IntegrationService integrationService;

    @Mock
    private IntegrationWorkflowService integrationWorkflowService;

    @Mock
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @Mock
    private McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private WorkflowService workflowService;

    private McpIntegrationInstanceConfigurationFacade mcpIntegrationInstanceConfigurationFacade;

    @BeforeEach
    void beforeEach() {
        mcpIntegrationInstanceConfigurationFacade = new McpIntegrationInstanceConfigurationFacadeImpl(
            componentConnectionFacade, connectionService, embeddedMcpReader, integrationInstanceConfigurationService,
            integrationInstanceConfigurationWorkflowService, integrationInstanceWorkflowService, integrationService,
            integrationWorkflowService, mcpIntegrationInstanceConfigurationService,
            mcpIntegrationInstanceConfigurationWorkflowService, mcpServerService, permissionService,
            workflowService);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);
        integrationInstanceConfiguration.setId(INTEGRATION_INSTANCE_CONFIGURATION_ID);
        integrationInstanceConfiguration.setIntegrationId(7L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            INTEGRATION_INSTANCE_CONFIGURATION_ID))
                .thenReturn(integrationInstanceConfiguration);

        when(embeddedMcpReader.getMcpServer(3L))
            .thenReturn(new McpServer("server", PlatformType.EMBEDDED, Environment.DEVELOPMENT));

        Integration integration = new Integration();

        integration.setComponentName("gmail");

        when(integrationService.getIntegration(7L)).thenReturn(integration);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getTasks(true)).thenReturn(List.of(mock(WorkflowTask.class)));
        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenReturn(
                List.of(
                    new ComponentConnection("gmail", 1, "gmail_1", "gmail", true),
                    new ComponentConnection("slack", 1, "slack_1", "slack", true)));

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            new McpIntegrationInstanceConfiguration(INTEGRATION_INSTANCE_CONFIGURATION_ID, 3L);

        mcpIntegrationInstanceConfiguration.setId(11L);

        when(mcpIntegrationInstanceConfigurationService.create(any())).thenReturn(mcpIntegrationInstanceConfiguration);
        when(mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(11L))
            .thenReturn(Optional.of(mcpIntegrationInstanceConfiguration));

        when(
            integrationInstanceConfigurationWorkflowService.create(any(IntegrationInstanceConfigurationWorkflow.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), 21L));
        when(
            integrationInstanceConfigurationWorkflowService.update(any(IntegrationInstanceConfigurationWorkflow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCreateStoresTheConfiguredInputsAndNonIntegrationConnections() {
        stubConnection(90L, "slack", Environment.DEVELOPMENT);

        mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            3L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID),
            List.of(slackWorkflowConfiguration(90L, Map.of("subject", "Hello"))));

        ArgumentCaptor<IntegrationInstanceConfigurationWorkflow> workflowArgumentCaptor =
            ArgumentCaptor.forClass(IntegrationInstanceConfigurationWorkflow.class);

        verify(integrationInstanceConfigurationWorkflowService).create(workflowArgumentCaptor.capture());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            workflowArgumentCaptor.getValue();

        assertThat(integrationInstanceConfigurationWorkflow.getConnections())
            .containsExactly(new IntegrationInstanceConfigurationWorkflowConnection(90L, "slack", "slack_1"));
        assertThat(integrationInstanceConfigurationWorkflow.getInputs()).isEqualTo(Map.of("subject", "Hello"));
    }

    @Test
    void testCreateRejectsAServerFromAnotherEnvironment() {
        when(embeddedMcpReader.getMcpServer(4L))
            .thenReturn(new McpServer("server", PlatformType.EMBEDDED, Environment.PRODUCTION));

        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                4L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");

        assertThatThrownBy(
            () -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
                4L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("environment");

        verify(mcpIntegrationInstanceConfigurationService, never()).create(any());
        verify(integrationInstanceConfigurationWorkflowService, never())
            .create(any(IntegrationInstanceConfigurationWorkflow.class));
    }

    @Test
    void testCreateRejectsAConnectionForTheIntegrationComponent() {
        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            3L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID),
            List.of(
                new WorkflowConfigurationDTO(
                    WORKFLOW_ID, List.of(new WorkflowConnectionDTO(80L, "gmail", "gmail_1")), Map.of()))))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("cannot be configured");

        verify(integrationInstanceConfigurationWorkflowService, never())
            .create(any(IntegrationInstanceConfigurationWorkflow.class));
    }

    @Test
    void testCreateRejectsAConnectionFromAnotherEnvironment() {
        stubConnection(90L, "slack", Environment.PRODUCTION);

        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            3L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID),
            List.of(slackWorkflowConfiguration(90L, Map.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Connection 90 cannot be used");
    }

    @Test
    void testCreateRejectsAConfigurationWithoutARequiredNonIntegrationConnection() {
        assertThatThrownBy(() -> mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            3L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID),
            List.of(new WorkflowConfigurationDTO(WORKFLOW_ID, List.of(), Map.of()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires a slack connection");
    }

    @Test
    void testCreateWithoutAConfigurationKeepsEmptyInputs() {
        mcpIntegrationInstanceConfigurationFacade.createMcpIntegrationInstanceConfiguration(
            3L, INTEGRATION_INSTANCE_CONFIGURATION_ID, List.of(WORKFLOW_ID));

        ArgumentCaptor<IntegrationInstanceConfigurationWorkflow> workflowArgumentCaptor =
            ArgumentCaptor.forClass(IntegrationInstanceConfigurationWorkflow.class);

        verify(integrationInstanceConfigurationWorkflowService).create(workflowArgumentCaptor.capture());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            workflowArgumentCaptor.getValue();

        assertThat(integrationInstanceConfigurationWorkflow.getConnections()).isEmpty();
        assertThat(integrationInstanceConfigurationWorkflow.getInputs()).isEmpty();
    }

    @Test
    void testUpdateAppliesTheConfigurationOfAKeptWorkflow() {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setId(21L);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
            INTEGRATION_INSTANCE_CONFIGURATION_ID);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of());
        integrationInstanceConfigurationWorkflow.setWorkflowId(WORKFLOW_ID);

        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
            INTEGRATION_INSTANCE_CONFIGURATION_ID))
                .thenReturn(List.of(integrationInstanceConfigurationWorkflow));

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            new McpIntegrationInstanceConfigurationWorkflow(11L, 21L);

        when(mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(11L))
                .thenReturn(List.of(mcpIntegrationInstanceConfigurationWorkflow));

        stubConnection(91L, "slack", Environment.DEVELOPMENT);

        mcpIntegrationInstanceConfigurationFacade.updateMcpIntegrationInstanceConfiguration(
            11L, List.of(WORKFLOW_ID), List.of(slackWorkflowConfiguration(91L, Map.of("subject", "Hi"))));

        ArgumentCaptor<IntegrationInstanceConfigurationWorkflow> workflowArgumentCaptor =
            ArgumentCaptor.forClass(IntegrationInstanceConfigurationWorkflow.class);

        verify(integrationInstanceConfigurationWorkflowService).update(workflowArgumentCaptor.capture());
        verify(mcpIntegrationInstanceConfigurationWorkflowService, never()).create(11L, 21L);

        IntegrationInstanceConfigurationWorkflow updatedIntegrationInstanceConfigurationWorkflow =
            workflowArgumentCaptor.getValue();

        assertThat(updatedIntegrationInstanceConfigurationWorkflow.getConnections())
            .containsExactly(new IntegrationInstanceConfigurationWorkflowConnection(91L, "slack", "slack_1"));
        assertThat(updatedIntegrationInstanceConfigurationWorkflow.getInputs()).isEqualTo(Map.of("subject", "Hi"));
    }

    private static WorkflowConfigurationDTO slackWorkflowConfiguration(long connectionId, Map<String, Object> inputs) {
        return new WorkflowConfigurationDTO(
            WORKFLOW_ID, List.of(new WorkflowConnectionDTO(connectionId, "slack", "slack_1")), inputs);
    }

    private static IntegrationInstanceConfigurationWorkflow withId(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, long id) {

        integrationInstanceConfigurationWorkflow.setId(id);

        return integrationInstanceConfigurationWorkflow;
    }

    private void stubConnection(long connectionId, String componentName, Environment environment) {
        Connection connection = new Connection();

        connection.setComponentName(componentName);
        connection.setEnvironmentId(environment.ordinal());
        connection.setId(connectionId);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }
}

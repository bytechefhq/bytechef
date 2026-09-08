/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * A workflow exposed as an MCP tool carries an optional name and description. Left unset, the workflow's own label and
 * description are what the model is given.
 *
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedMcpWorkflowToolFacadeTest {

    private static final String WORKFLOW_DESCRIPTION = "Sends an email to a customer";

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService =
        mock(IntegrationInstanceConfigurationWorkflowService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService =
        mock(McpIntegrationInstanceConfigurationWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final EmbeddedMcpToolFacade embeddedMcpToolFacade = new EmbeddedMcpToolFacade(
        mock(ClusterElementDefinitionFacade.class), mock(ClusterElementDefinitionService.class),
        mock(ComponentDefinitionService.class), mock(ConnectedUserService.class), mock(Evaluator.class),
        integrationInstanceConfigurationService, integrationInstanceConfigurationWorkflowService,
        integrationInstanceService, mock(IntegrationInstanceWorkflowService.class), integrationService,
        mock(JobSyncExecutor.class), mock(JwtTokenService.class), mock(McpComponentService.class),
        mcpIntegrationInstanceConfigurationWorkflowService, mock(McpIntegrationInstanceToolService.class),
        mock(McpServerService.class), mock(PrincipalJobFacade.class), "http://localhost:8080",
        mock(TaskExecutionService.class), mock(TaskFileStorage.class), workflowService);

    @Test
    void testToolNameFallsBackToTheWorkflowLabel() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals("Send_Email", toolDefinition.name());
    }

    @Test
    void testToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "sendEmail"));

        assertEquals("sendEmail", toolDefinition.name());
    }

    @Test
    void testToolDescriptionFallsBackToTheWorkflowDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals(WORKFLOW_DESCRIPTION, toolDefinition.description());
    }

    @Test
    void testToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "Emails the customer"));

        assertEquals("Emails the customer", toolDefinition.description());
    }

    @Test
    void testToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "   "));

        assertEquals(WORKFLOW_DESCRIPTION, toolDefinition.description());
    }

    private ToolDefinition getToolDefinition(Map<String, Object> parameters) {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            new McpIntegrationInstanceConfiguration();

        mcpIntegrationInstanceConfiguration.setId(1L);
        mcpIntegrationInstanceConfiguration.setMcpServerId(1L);

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            new McpIntegrationInstanceConfigurationWorkflow();

        mcpIntegrationInstanceConfigurationWorkflow.setId(1L);
        mcpIntegrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationWorkflowId(1L);
        mcpIntegrationInstanceConfigurationWorkflow.setParameters(parameters);

        when(
            mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(1L))
                    .thenReturn(List.of(mcpIntegrationInstanceConfigurationWorkflow));

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            mock(IntegrationInstanceConfigurationWorkflow.class);

        when(integrationInstanceConfigurationWorkflow.isEnabled()).thenReturn(true);
        when(integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId()).thenReturn(1L);
        when(integrationInstanceConfigurationWorkflow.getWorkflowId()).thenReturn("workflow-1");
        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(1L))
            .thenReturn(integrationInstanceConfigurationWorkflow);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            mock(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfiguration.getIntegrationId()).thenReturn(1L);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(1L))
            .thenReturn(integrationInstanceConfiguration);

        Integration integration = mock(Integration.class);

        when(integration.getComponentName()).thenReturn("googleMail");
        when(integration.getId()).thenReturn(1L);
        when(integrationService.getIntegration(1L)).thenReturn(integration);

        when(integrationInstanceService.fetchIntegrationInstance(anyLong(), anyString(), any()))
            .thenReturn(Optional.empty());

        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "parameters", Map.of(), "type", "workflow/v1/newWorkflowCall"));

        Workflow workflow = mock(Workflow.class);

        when(workflow.getLabel()).thenReturn("Send Email");
        when(workflow.getDescription()).thenReturn(WORKFLOW_DESCRIPTION);
        when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(workflowTrigger));
        when(workflowService.getWorkflow("workflow-1")).thenReturn(workflow);

        List<ToolCallback> toolCallbacks = embeddedMcpToolFacade.getFunctionToolCallbacks(
            mcpIntegrationInstanceConfiguration, "external-user-1", Environment.PRODUCTION, "000001");

        assertEquals(1, toolCallbacks.size());

        ToolCallback toolCallback = toolCallbacks.getFirst();

        return toolCallback.getToolDefinition();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.workflow.JobInputConstants;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Mirrors {@code AutomationMcpToolFacadeWorkflowToolsTest} (CE): pins that calling an Embedded MCP workflow tool seeds
 * the reserved {@code __triggerName} job input alongside the trigger-name-keyed payload, so a workflow launched this
 * way can drive an agent workflow's {@code branch_in} dispatcher (keyed on {@code ${__triggerName}}) the same as every
 * other job-creation path (see {@code JobInputConstants}).
 *
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SuppressWarnings("unchecked")
class EmbeddedMcpToolFacadeWorkflowToolsTest {

    private static final String NEW_WORKFLOW_CALL_TYPE = "workflow/v1/newWorkflowCall";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService =
        mock(IntegrationInstanceConfigurationWorkflowService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService =
        mock(McpIntegrationInstanceConfigurationWorkflowService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);
    private final ToolExecutionRecorder toolExecutionRecorder = mock(ToolExecutionRecorder.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final EmbeddedMcpToolFacade facade = new EmbeddedMcpToolFacade(
        mock(ObjectProvider.class), mock(ClusterElementDefinitionFacade.class),
        mock(ClusterElementDefinitionService.class), mock(ComponentDefinitionService.class), connectedUserService,
        mock(Evaluator.class), integrationInstanceConfigurationService,
        integrationInstanceConfigurationWorkflowService, integrationInstanceService,
        integrationInstanceWorkflowService, integrationService, mock(JobCompletionAwaiter.class),
        mock(JobResumeFacade.class), mock(JobService.class), mock(JwtTokenService.class),
        mock(McpComponentService.class), mcpIntegrationInstanceConfigurationWorkflowService,
        mock(McpIntegrationInstanceToolService.class), mcpServerService, mock(ObjectProvider.class),
        principalJobFacade, "https://example.com", mock(TaskExecutionService.class), mock(TaskFileStorage.class),
        toolExecutionRecorder, workflowService);

    @Test
    void testCallOfWorkflowToolSeedsReservedTriggerNameInput() {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            new McpIntegrationInstanceConfiguration(1L, 50L, 30L);

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            new McpIntegrationInstanceConfigurationWorkflow(1L, 21L);

        mcpIntegrationInstanceConfigurationWorkflow.setId(60L);
        mcpIntegrationInstanceConfigurationWorkflow.setParameters(
            Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"));

        when(mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(1L))
                .thenReturn(List.of(mcpIntegrationInstanceConfigurationWorkflow));

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setId(21L);
        integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(50L);
        integrationInstanceConfigurationWorkflow.setWorkflowId("wf-1");
        integrationInstanceConfigurationWorkflow.setEnabled(true);
        integrationInstanceConfigurationWorkflow.setInputs(Map.of());

        when(integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(21L))
            .thenReturn(integrationInstanceConfigurationWorkflow);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setId(50L);
        integrationInstanceConfiguration.setIntegrationId(70L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(50L))
            .thenReturn(integrationInstanceConfiguration);

        Integration integration = new Integration();

        integration.setId(70L);
        integration.setComponentName("myComponent");

        when(integrationService.getIntegration(70L)).thenReturn(integration);

        ConnectedUser connectedUser = new ConnectedUser(
            Map.of(), null, true, "external-user", 80L, "external-user", 0);

        when(connectedUserService.fetchConnectedUser("external-user", Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUser));

        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setId(90L);
        integrationInstance.setConnectedUserId(80L);
        integrationInstance.setConnectionId(100L);
        integrationInstance.setIntegrationInstanceConfigurationId(50L);

        when(integrationInstanceService.fetchIntegrationInstance(80L, "myComponent", Environment.PRODUCTION))
            .thenReturn(Optional.of(integrationInstance));

        when(mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(60L))
            .thenReturn(Optional.of(mcpIntegrationInstanceConfigurationWorkflow));

        IntegrationInstanceWorkflow integrationInstanceWorkflow = new IntegrationInstanceWorkflow();

        integrationInstanceWorkflow.setIntegrationInstanceId(90L);
        integrationInstanceWorkflow.setIntegrationInstanceConfigurationWorkflowId(21L);
        integrationInstanceWorkflow.setEnabled(true);

        when(integrationInstanceWorkflowService.fetchIntegrationInstanceWorkflow(90L, 21L))
            .thenReturn(Optional.of(integrationInstanceWorkflow));

        when(workflowService.getWorkflow("wf-1")).thenReturn(mock(Workflow.class));

        when(mcpServerService.getMcpServer(30L)).thenReturn(
            new McpServer("test-server", PlatformType.EMBEDDED, Environment.PRODUCTION, true));

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(toolExecutionRecorder.record(any(), any(Supplier.class))).thenReturn(Map.of("ok", true));

        try (MockedStatic<WorkflowTrigger> workflowTriggerMockedStatic = mockStatic(WorkflowTrigger.class);
            MockedStatic<WorkflowNodeType> workflowNodeTypeMockedStatic = mockStatic(WorkflowNodeType.class)) {

            WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);
            WorkflowNodeType workflowNodeType = mock(WorkflowNodeType.class);

            when(workflowTrigger.getType()).thenReturn(NEW_WORKFLOW_CALL_TYPE);
            when(workflowTrigger.getName()).thenReturn("newWorkflowCall_1");
            when(workflowNodeType.name()).thenReturn(WorkflowConstants.WORKFLOW);
            when(workflowNodeType.operation()).thenReturn(WorkflowConstants.NEW_WORKFLOW_CALL);

            workflowTriggerMockedStatic.when(() -> WorkflowTrigger.of(any(Workflow.class)))
                .thenReturn(List.of(workflowTrigger));
            workflowNodeTypeMockedStatic.when(() -> WorkflowNodeType.ofType(NEW_WORKFLOW_CALL_TYPE))
                .thenReturn(workflowNodeType);

            List<ToolCallback> toolCallbacks = facade.getFunctionToolCallbacks(
                mcpIntegrationInstanceConfiguration, "external-user", Environment.PRODUCTION, "tenant-1");

            assertThat(toolCallbacks).hasSize(1);

            ToolCallback toolCallback = toolCallbacks.getFirst();

            toolCallback.call("{}");

            ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
                ArgumentCaptor.forClass(JobParametersDTO.class);

            verify(principalJobFacade).createJob(jobParametersDTOArgumentCaptor.capture(), anyLong(), any());

            JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

            assertThat(jobParametersDTO.getInputs())
                .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, "newWorkflowCall_1")
                .containsKey("newWorkflowCall_1");
        }
    }
}

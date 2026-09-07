/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedMcpToolFacadeTest {

    private static final String CLUSTER_ELEMENT_DESCRIPTION =
        "The POST method submits an entity to the specified resource.";

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);

    private final EmbeddedMcpToolFacade embeddedMcpToolFacade = new EmbeddedMcpToolFacade(
        mock(ClusterElementDefinitionFacade.class), clusterElementDefinitionService,
        mock(ComponentDefinitionService.class), connectedUserService, mock(Evaluator.class),
        mock(IntegrationInstanceConfigurationService.class),
        mock(IntegrationInstanceConfigurationWorkflowService.class), mock(IntegrationInstanceService.class),
        mock(IntegrationInstanceWorkflowService.class), mock(IntegrationService.class), mock(JobSyncExecutor.class),
        mock(JwtTokenService.class), mcpComponentService,
        mock(McpIntegrationInstanceConfigurationWorkflowService.class), mock(McpIntegrationInstanceToolService.class),
        mock(McpServerService.class), mock(PrincipalJobFacade.class), "http://localhost:8080",
        mock(TaskExecutionService.class), mock(TaskFileStorage.class), mock(WorkflowService.class));

    // The tool name is optional, so a tool configured without one still has to reach the model under a callable
    // name derived from the component and the cluster element.
    @Test
    void testToolNameFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals("HTTPCLIENT_POST", toolDefinition.name());
    }

    @Test
    void testToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolName", "postThing"));

        assertEquals("postThing", toolDefinition.name());
    }

    // The description is optional, so a tool configured without one is described to the model by the cluster
    // element's own description.
    @Test
    void testToolDescriptionFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of());

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    @Test
    void testToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "Posts a thing"));

        assertEquals("Posts a thing", toolDefinition.description());
    }

    @Test
    void testToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getToolDefinition(Map.of("toolDescription", "   "));

        assertEquals(CLUSTER_ELEMENT_DESCRIPTION, toolDefinition.description());
    }

    private ToolDefinition getToolDefinition(Map<String, Object> parameters) {
        McpTool mcpTool = new McpTool();

        mcpTool.setMcpComponentId(1L);
        mcpTool.setName("post");
        mcpTool.setParameters(parameters);

        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setComponentName("httpClient");
        mcpComponent.setComponentVersion(1);
        mcpComponent.setMcpServerId(1L);

        when(mcpComponentService.getMcpComponent(1L)).thenReturn(mcpComponent);
        when(connectedUserService.fetchConnectedUser(anyString(), any())).thenReturn(Optional.empty());

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("httpClient");
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn("post");
        when(clusterElementDefinition.getDescription()).thenReturn(CLUSTER_ELEMENT_DESCRIPTION);
        when(clusterElementDefinitionService.getClusterElementDefinition("httpClient", 1, "post"))
            .thenReturn(clusterElementDefinition);

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback =
            embeddedMcpToolFacade.getFunctionToolCallback(mcpTool, "externalUserId", Environment.PRODUCTION, "tenant");

        assertNotNull(functionToolCallback);

        return functionToolCallback.getToolDefinition();
    }
}

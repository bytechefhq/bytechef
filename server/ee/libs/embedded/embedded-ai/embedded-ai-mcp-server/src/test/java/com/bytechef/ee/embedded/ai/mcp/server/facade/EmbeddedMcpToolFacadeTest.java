/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class EmbeddedMcpToolFacadeTest {

    private static final String CLUSTER_ELEMENT_DESCRIPTION =
        "The POST method submits an entity to the specified resource.";

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade =
        mock(ClusterElementDefinitionFacade.class);
    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);

    private final EmbeddedMcpToolFacade embeddedMcpToolFacade = new EmbeddedMcpToolFacade(
        clusterElementDefinitionFacade, clusterElementDefinitionService, componentDefinitionService,
        connectedUserService, mock(Evaluator.class), mock(IntegrationInstanceConfigurationService.class),
        mock(IntegrationInstanceConfigurationWorkflowService.class), mock(IntegrationInstanceService.class),
        mock(IntegrationInstanceWorkflowService.class), mock(IntegrationService.class), mock(JobSyncExecutor.class),
        mock(JwtTokenService.class), mcpComponentService,
        mock(McpIntegrationInstanceConfigurationWorkflowService.class), mock(McpIntegrationInstanceToolService.class),
        mcpServerService, mock(PrincipalJobFacade.class), "http://localhost:8080", mock(TaskExecutionService.class),
        mock(TaskFileStorage.class), mock(WorkflowService.class));

    @Test
    void testToolCallReadsTheServerThroughTheUngatedEnabledEmbeddedServersLookup() {
        McpServer mcpServer = new McpServer();

        mcpServer.setId(1L);
        mcpServer.setEnabled(true);

        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of(mcpServer));

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinitionService.getComponentDefinition("httpClient", 1)).thenReturn(componentDefinition);
        when(clusterElementDefinitionFacade.executeTool(eq("httpClient"), eq(1), eq("post"), anyMap(), isNull()))
            .thenReturn("posted");

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = getFunctionToolCallback(Map.of());

        assertEquals("\"posted\"", functionToolCallback.call("{}"));
        verify(mcpServerService, never()).getMcpServer(anyLong());
    }

    @Test
    void testToolCallThrowsWhenTheMcpServerIsNotAmongTheEnabledEmbeddedServers() {
        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)).thenReturn(List.of());

        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        when(componentDefinitionService.getComponentDefinition("httpClient", 1)).thenReturn(componentDefinition);

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = getFunctionToolCallback(Map.of());

        ToolExecutionException toolExecutionException = assertThrows(
            ToolExecutionException.class, () -> functionToolCallback.call("{}"));

        assertEquals("MCP server is disabled", toolExecutionException.getCause()
            .getMessage());
        verify(mcpServerService, never()).getMcpServer(anyLong());
    }

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
        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback = getFunctionToolCallback(parameters);

        return functionToolCallback.getToolDefinition();
    }

    private FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(Map<String, Object> parameters) {
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

        return functionToolCallback;
    }
}

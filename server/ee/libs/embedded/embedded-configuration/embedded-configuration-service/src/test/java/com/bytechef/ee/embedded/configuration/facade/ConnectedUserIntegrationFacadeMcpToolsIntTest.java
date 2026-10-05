/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationFacadeMcpToolsIntTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);
    private final ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade = createFacade();

    @Test
    void testGetMcpToolsUsesClusterElementTitleAsLabel() {
        mockMcpComponent(1L, 100L, PlatformType.EMBEDDED, true);
        mockMcpTool(1L, 1051L, "sendEmail", "Send Email", "Send an email");

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpToolInfos =
            connectedUserIntegrationFacade.getMcpTools("gmail");

        assertEquals(1, mcpToolInfos.size());

        ConnectedUserIntegrationDTO.McpToolInfo mcpToolInfo = mcpToolInfos.getFirst();

        assertEquals(1051L, mcpToolInfo.id());
        assertEquals("sendEmail", mcpToolInfo.name());
        assertEquals("Send Email", mcpToolInfo.label());
        assertEquals("Send an email", mcpToolInfo.description());
    }

    @Test
    void testGetMcpToolsKeepsNullLabelWhenClusterElementHasNoTitle() {
        mockMcpComponent(1L, 100L, PlatformType.EMBEDDED, true);
        mockMcpTool(1L, 1051L, "sendEmail", null, "Send an email");

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpToolInfos =
            connectedUserIntegrationFacade.getMcpTools("gmail");

        ConnectedUserIntegrationDTO.McpToolInfo mcpToolInfo = mcpToolInfos.getFirst();

        assertEquals("sendEmail", mcpToolInfo.name());
        assertNull(mcpToolInfo.label());
    }

    @Test
    void testGetMcpToolsSkipsToolsOfDisabledMcpServer() {
        mockMcpComponent(1L, 100L, PlatformType.EMBEDDED, false);
        mockMcpTool(1L, 1051L, "sendEmail", "Send Email", "Send an email");

        assertTrue(connectedUserIntegrationFacade.getMcpTools("gmail")
            .isEmpty());
    }

    @Test
    void testGetMcpToolsSkipsToolsOfNonEmbeddedMcpServer() {
        mockMcpComponent(1L, 100L, PlatformType.AUTOMATION, true);
        mockMcpTool(1L, 1051L, "sendEmail", "Send Email", "Send an email");

        assertTrue(connectedUserIntegrationFacade.getMcpTools("gmail")
            .isEmpty());
    }

    private void mockMcpComponent(long mcpComponentId, long mcpServerId, PlatformType type, boolean enabled) {
        McpComponent mcpComponent = mock(McpComponent.class);

        when(mcpComponent.getId()).thenReturn(mcpComponentId);
        when(mcpComponent.getMcpServerId()).thenReturn(mcpServerId);
        when(mcpComponent.getComponentName()).thenReturn("gmail");
        when(mcpComponent.getComponentVersion()).thenReturn(1);

        McpServer mcpServer = mock(McpServer.class);

        when(mcpServer.getType()).thenReturn(type);
        when(mcpServer.isEnabled()).thenReturn(enabled);

        when(mcpComponentService.getMcpComponentsByComponentName("gmail")).thenReturn(List.of(mcpComponent));
        when(mcpServerService.getMcpServer(mcpServerId)).thenReturn(mcpServer);
    }

    private void mockMcpTool(long mcpComponentId, long mcpToolId, String name, String title, String description) {
        McpTool mcpTool = mock(McpTool.class);

        when(mcpTool.getId()).thenReturn(mcpToolId);
        when(mcpTool.getName()).thenReturn(name);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getTitle()).thenReturn(title);
        when(clusterElementDefinition.getDescription()).thenReturn(description);

        when(mcpToolService.getMcpComponentMcpTools(mcpComponentId)).thenReturn(List.of(mcpTool));
        when(clusterElementDefinitionService.getClusterElementDefinition("gmail", 1, name))
            .thenReturn(clusterElementDefinition);
    }

    private ConnectedUserIntegrationFacadeImpl createFacade() {
        return new ConnectedUserIntegrationFacadeImpl(
            clusterElementDefinitionService, mock(ComponentDefinitionService.class),
            mock(ConnectedUserService.class), mock(ConnectionFacade.class), mock(ConnectionService.class),
            mock(EmbeddedPermissionEvaluator.class), mock(IntegrationInstanceConfigurationFacade.class),
            mock(IntegrationInstanceConfigurationService.class),
            mock(IntegrationInstanceConfigurationWorkflowService.class), mock(IntegrationInstanceService.class),
            mock(IntegrationService.class), mcpComponentService,
            mock(McpIntegrationInstanceConfigurationService.class),
            mock(McpIntegrationInstanceConfigurationWorkflowService.class),
            mock(McpIntegrationInstanceToolService.class), mcpServerService, mcpToolService,
            mock(OAuth2ParametersFacade.class), mock(OAuth2Service.class),
            mock(IntegrationInstanceWorkflowService.class), mock(IntegrationWorkflowService.class),
            mock(WorkflowService.class));
    }
}

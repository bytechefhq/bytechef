/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.domain.Connection;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The MCP tests prove the connected-user integration facade reads MCP server/tool enablement through the ungated
 * {@link McpServerService#getEnabledMcpServers(PlatformType)} path instead of the connected-user-gated
 * {@code getMcpServer}/{@code fetchMcpTool} service methods.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationFacadeTest {

    private static final long ENABLED_EMBEDDED_MCP_SERVER_ID = 1L;
    private static final long DISABLED_EMBEDDED_MCP_SERVER_ID = 2L;
    private static final long AUTOMATION_MCP_SERVER_ID = 3L;

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final McpComponentService mcpComponentService = mock(McpComponentService.class);
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService =
        mock(McpIntegrationInstanceToolService.class);
    private final McpServerService mcpServerService = mock(McpServerService.class);
    private final McpToolService mcpToolService = mock(McpToolService.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);
    private final ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade = createFacade();

    @Test
    void testGetEnabledEmbeddedMcpServerIdsUsesTheServicesEnabledEmbeddedServers() {
        McpServer enabledEmbeddedServer = mcpServer(ENABLED_EMBEDDED_MCP_SERVER_ID);

        when(mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED))
            .thenReturn(List.of(enabledEmbeddedServer));

        Set<Long> enabledEmbeddedMcpServerIds = connectedUserIntegrationFacade.getEnabledEmbeddedMcpServerIds();

        assertThat(enabledEmbeddedMcpServerIds).containsExactly(ENABLED_EMBEDDED_MCP_SERVER_ID);
        verify(mcpServerService, never()).getMcpServer(anyLong());
    }

    @Test
    void testGetMcpToolsOnlyIncludesToolsFromComponentsOnEnabledEmbeddedServers() {
        McpComponent enabledEmbeddedComponent = mcpComponent(10L, ENABLED_EMBEDDED_MCP_SERVER_ID);
        McpComponent disabledEmbeddedComponent = mcpComponent(20L, DISABLED_EMBEDDED_MCP_SERVER_ID);
        McpComponent automationComponent = mcpComponent(30L, AUTOMATION_MCP_SERVER_ID);

        when(mcpComponentService.getMcpComponentsByComponentName("slack"))
            .thenReturn(List.of(enabledEmbeddedComponent, disabledEmbeddedComponent, automationComponent));

        McpTool enabledEmbeddedTool = new McpTool(100L, "sendMessage", Map.of(), enabledEmbeddedComponent.getId());

        when(mcpToolService.getMcpComponentMcpTools(enabledEmbeddedComponent.getId()))
            .thenReturn(List.of(enabledEmbeddedTool));

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getDescription()).thenReturn("Sends a message");
        when(
            clusterElementDefinitionService.getClusterElementDefinition("slack", 1, "sendMessage"))
                .thenReturn(clusterElementDefinition);

        List<ConnectedUserIntegrationDTO.McpToolInfo> mcpTools = connectedUserIntegrationFacade.getMcpTools(
            "slack", Set.of(ENABLED_EMBEDDED_MCP_SERVER_ID));

        assertThat(mcpTools).containsExactly(
            new ConnectedUserIntegrationDTO.McpToolInfo(100L, "sendMessage", null, "Sends a message"));
        verify(mcpToolService, never()).getMcpComponentMcpTools(disabledEmbeddedComponent.getId());
        verify(mcpToolService, never()).getMcpComponentMcpTools(automationComponent.getId());
        verify(mcpServerService, never()).getMcpServer(anyLong());
    }

    @Test
    void testAttachInstanceMcpDataFiltersInstanceToolsByTheEnabledToolIdSet() {
        long integrationInstanceId = 5L;

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getId()).thenReturn(integrationInstanceId);

        ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance connectedUserIntegrationInstance =
            new ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance(
                mock(Connection.class), integrationInstance, List.of());

        McpIntegrationInstanceTool enabledTool = new McpIntegrationInstanceTool(integrationInstanceId, 100L, true);
        McpIntegrationInstanceTool disabledEmbeddedServerTool =
            new McpIntegrationInstanceTool(integrationInstanceId, 200L, true);

        when(mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId))
            .thenReturn(List.of(enabledTool, disabledEmbeddedServerTool));
        when(integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId))
            .thenReturn(List.of());

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        List<ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance> result = connectedUserIntegrationFacade
            .attachInstanceMcpData(
                List.of(connectedUserIntegrationInstance), connectedUser,
                Set.of(ENABLED_EMBEDDED_MCP_SERVER_ID), Set.of(100L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()
            .mcpTools()).containsExactly(new ConnectedUserIntegrationDTO.McpInstanceToolInfo(100L, true));
        verify(mcpToolService, never()).fetchMcpTool(anyLong());
    }

    @Test
    void testDeleteIntegrationInstanceOfTheConnectedUser() {
        mockIntegrationInstance(7L, 1L);
        mockConnectedUser("external-user-1", 1L);

        connectedUserIntegrationFacade.deleteIntegrationInstance("external-user-1", 7L);

        verify(integrationInstanceWorkflowService).deleteByIntegrationInstanceId(7L);
        verify(integrationInstanceService).delete(7L);
    }

    @Test
    void testDeleteIntegrationInstanceOfAnotherConnectedUserIsRefusedBeforeAnythingIsDeleted() {
        mockIntegrationInstance(7L, 1L);
        mockConnectedUser("external-user-2", 2L);

        assertThatThrownBy(() -> connectedUserIntegrationFacade.deleteIntegrationInstance("external-user-2", 7L))
            .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).deleteByIntegrationInstanceId(anyLong());
        verify(integrationInstanceService, never()).delete(anyLong());
    }

    @Test
    void testDeleteIntegrationInstanceWithoutAConnectedUserIsRefusedBeforeAnythingIsDeleted() {
        mockIntegrationInstance(7L, 1L);

        when(connectedUserService.fetchConnectedUser("external-user-3", Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectedUserIntegrationFacade.deleteIntegrationInstance("external-user-3", 7L))
            .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).deleteByIntegrationInstanceId(anyLong());
        verify(integrationInstanceService, never()).delete(anyLong());
    }

    private void mockConnectedUser(String externalUserId, long connectedUserId) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getExternalId()).thenReturn(externalUserId);
        when(connectedUser.getId()).thenReturn(connectedUserId);

        when(connectedUserService.fetchConnectedUser(externalUserId, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUser));
    }

    private void mockIntegrationInstance(long integrationInstanceId, long connectedUserId) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(connectedUserId);
        integrationInstance.setIntegrationInstanceConfigurationId(3L);

        when(integrationInstanceService.getIntegrationInstance(integrationInstanceId))
            .thenReturn(integrationInstance);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(3L))
            .thenReturn(integrationInstanceConfiguration);
    }

    private static McpServer mcpServer(long id) {
        McpServer mcpServer = new McpServer("embedded-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT, true);

        mcpServer.setId(id);

        return mcpServer;
    }

    private static McpComponent mcpComponent(long id, long mcpServerId) {
        McpComponent mcpComponent = new McpComponent("slack", 1, mcpServerId, null);

        mcpComponent.setId(id);

        return mcpComponent;
    }

    private ConnectedUserIntegrationFacadeImpl createFacade() {
        return new ConnectedUserIntegrationFacadeImpl(
            clusterElementDefinitionService, mock(ComponentDefinitionService.class), connectedUserService,
            mock(ConnectionFacade.class), mock(ConnectionService.class), mock(EmbeddedPermissionEvaluator.class),
            mock(IntegrationInstanceConfigurationFacade.class), integrationInstanceConfigurationService,
            mock(IntegrationInstanceConfigurationWorkflowService.class), integrationInstanceService,
            mock(IntegrationService.class), mcpComponentService,
            mock(McpIntegrationInstanceConfigurationService.class),
            mock(McpIntegrationInstanceConfigurationWorkflowService.class), mcpIntegrationInstanceToolService,
            mcpServerService, mcpToolService, mock(OAuth2ParametersFacade.class), mock(OAuth2Service.class),
            integrationInstanceWorkflowService, mock(IntegrationWorkflowService.class), mock(WorkflowService.class));
    }
}

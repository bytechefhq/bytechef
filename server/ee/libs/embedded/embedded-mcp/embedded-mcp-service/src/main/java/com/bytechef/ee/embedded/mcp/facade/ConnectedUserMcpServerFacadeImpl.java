/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.mcp.dto.ConnectedUserMcpServerToolDTO;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserMcpServerFacadeImpl implements ConnectedUserMcpServerFacade {

    private final IntegrationInstanceService integrationInstanceService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public ConnectedUserMcpServerFacadeImpl(
        IntegrationInstanceService integrationInstanceService, McpComponentService mcpComponentService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpServerService mcpServerService,
        McpToolService mcpToolService) {

        this.integrationInstanceService = integrationInstanceService;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public void deleteConnectedUserMcpServer(long connectedUserId, long mcpServerId) {
        // Remove every per-user tool row whose underlying McpComponent points at the given server.
        // Since the read query rebuilds the (server -> tools) shape from these rows, removing them
        // makes the server vanish from this user's MCP Servers tab without any soft-delete flag.
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceTool> toolRows = mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstance.getId());

            for (McpIntegrationInstanceTool toolRow : toolRows) {
                Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(toolRow.getMcpToolId());

                if (mcpToolOptional.isEmpty()) {
                    continue;
                }

                McpTool mcpTool = mcpToolOptional.get();

                McpComponent mcpComponent = mcpComponentCache.computeIfAbsent(
                    mcpTool.getMcpComponentId(), mcpComponentService::getMcpComponent);

                if (mcpComponent.getMcpServerId() != mcpServerId) {
                    continue;
                }

                mcpIntegrationInstanceToolService.delete(toolRow.getId());
            }
        }
    }

    @Override
    public void enableConnectedUserMcpServer(long connectedUserId, long mcpServerId, boolean enable) {
        // Bulk-flip every per-user tool whose underlying McpComponent points at the given server.
        // Computed "server enabled for user" = any tool enabled; toggling the card thus disables or
        // re-enables the whole group rather than introducing a separate per-(user, server) state row.
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceTool> toolRows = mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstance.getId());

            for (McpIntegrationInstanceTool toolRow : toolRows) {
                Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(toolRow.getMcpToolId());

                if (mcpToolOptional.isEmpty()) {
                    continue;
                }

                McpTool mcpTool = mcpToolOptional.get();

                McpComponent mcpComponent = mcpComponentCache.computeIfAbsent(
                    mcpTool.getMcpComponentId(), mcpComponentService::getMcpComponent);

                if (mcpComponent.getMcpServerId() != mcpServerId) {
                    continue;
                }

                mcpIntegrationInstanceToolService.updateEnabled(toolRow.getId(), enable);
            }
        }
    }

    @Override
    public void enableMcpTool(long mcpIntegrationInstanceToolId, boolean enable) {
        mcpIntegrationInstanceToolService.updateEnabled(mcpIntegrationInstanceToolId, enable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserMcpServerDTO> getConnectedUserMcpServers(long connectedUserId) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        // Group per-tool rows by the MCP server they ultimately belong to so the UI can render one
        // collapsible card per server with the user's enabled-tool list inside.
        Map<Long, List<ConnectedUserMcpServerToolDTO>> toolsByServerId = new HashMap<>();
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceTool> toolRows = mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstance.getId());

            for (McpIntegrationInstanceTool toolRow : toolRows) {
                Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(toolRow.getMcpToolId());

                if (mcpToolOptional.isEmpty()) {
                    continue;
                }

                McpTool mcpTool = mcpToolOptional.get();

                McpComponent mcpComponent = mcpComponentCache.computeIfAbsent(
                    mcpTool.getMcpComponentId(), mcpComponentService::getMcpComponent);

                toolsByServerId.computeIfAbsent(mcpComponent.getMcpServerId(), key -> new ArrayList<>())
                    .add(new ConnectedUserMcpServerToolDTO(
                        toolRow.getId(), mcpComponent.getComponentName(), mcpComponent.getComponentVersion(),
                        integrationInstance.getId(), mcpTool.getName(), toolRow.isEnabled()));
            }
        }

        List<ConnectedUserMcpServerDTO> connectedUserMcpServers = new ArrayList<>();

        for (Map.Entry<Long, List<ConnectedUserMcpServerToolDTO>> entry : toolsByServerId.entrySet()) {
            McpServer mcpServer = mcpServerService.getMcpServer(entry.getKey());

            List<ConnectedUserMcpServerToolDTO> tools = entry.getValue();

            tools.sort(Comparator.comparing(ConnectedUserMcpServerToolDTO::name));

            // "enabled for user" is computed per-user, not taken from the workspace-level flag: the
            // server is considered active for this user when at least one of their tools is enabled.
            boolean enabledForUser = tools.stream()
                .anyMatch(ConnectedUserMcpServerToolDTO::enabled);

            connectedUserMcpServers.add(new ConnectedUserMcpServerDTO(
                mcpServer.getId(), mcpServer.getName(), enabledForUser, mcpServer.getEnvironmentId(),
                mcpServer.getLastModifiedDate(), tools));
        }

        connectedUserMcpServers.sort(Comparator.comparing(ConnectedUserMcpServerDTO::name));

        return connectedUserMcpServers;
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
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
class McpIntegrationInstanceToolFacadeImpl implements McpIntegrationInstanceToolFacade {

    private final ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceToolFacadeImpl(
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        McpComponentService mcpComponentService, McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpServerService mcpServerService, McpToolService mcpToolService) {

        this.connectedUserIntegrationInstanceFacade = connectedUserIntegrationInstanceFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    @PreAuthorize("isTenantAdmin() or isConnectedUser()")
    public void enableMcpIntegrationInstanceTool(long integrationInstanceId, long mcpToolId, boolean enable) {
        connectedUserIntegrationInstanceFacade.validateCurrentPrincipalIntegrationInstanceOwnership(
            integrationInstanceId);

        McpTool mcpTool = mcpToolService.fetchMcpTool(mcpToolId)
            .orElseThrow(() -> new IllegalArgumentException("MCP tool not found: " + mcpToolId));

        if (!mcpTool.isEnabled()) {
            throw new IllegalArgumentException("MCP tool %s is disabled".formatted(mcpToolId));
        }

        if (!belongsToIntegrationInstance(mcpTool, integrationInstanceId)) {
            throw new IllegalArgumentException(
                "MCP tool %s does not belong to integration instance %s".formatted(mcpToolId, integrationInstanceId));
        }

        mcpIntegrationInstanceToolService
            .fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId)
            .ifPresentOrElse(
                mcpIntegrationInstanceTool -> mcpIntegrationInstanceToolService
                    .updateEnabled(mcpIntegrationInstanceTool.getId(), enable),
                () -> mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
                    integrationInstanceId, mcpToolId, enable));
    }

    private boolean belongsToIntegrationInstance(McpTool mcpTool, long integrationInstanceId) {
        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        McpServer mcpServer = mcpServerService.getMcpServer(mcpComponent.getMcpServerId());

        if (mcpServer.getType() != PlatformType.EMBEDDED) {
            return false;
        }

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstance.getIntegrationInstanceConfigurationId());

        Integration integration = integrationService.getIntegration(
            integrationInstanceConfiguration.getIntegrationId());

        return Objects.equals(mcpComponent.getComponentName(), integration.getComponentName());
    }
}

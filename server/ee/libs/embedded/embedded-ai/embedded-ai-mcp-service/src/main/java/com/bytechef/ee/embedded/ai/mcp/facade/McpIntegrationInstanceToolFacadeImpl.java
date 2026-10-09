/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceToolFacadeImpl(
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpToolService mcpToolService) {

        this.connectedUserIntegrationInstanceFacade = connectedUserIntegrationInstanceFacade;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
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

        mcpIntegrationInstanceToolService
            .fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId)
            .ifPresentOrElse(
                mcpIntegrationInstanceTool -> mcpIntegrationInstanceToolService
                    .updateEnabled(mcpIntegrationInstanceTool.getId(), enable),
                () -> mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
                    integrationInstanceId, mcpToolId, enable));
    }
}

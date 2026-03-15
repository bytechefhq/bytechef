/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceToolFacadeImpl(
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService) {

        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
    }

    @Override
    public void enableMcpIntegrationInstanceTool(long integrationInstanceId, long mcpToolId, boolean enable) {
        mcpIntegrationInstanceToolService
            .fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId)
            .ifPresentOrElse(
                mcpIntegrationInstanceTool -> mcpIntegrationInstanceToolService
                    .updateEnabled(mcpIntegrationInstanceTool.getId(), enable),
                () -> mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
                    integrationInstanceId, mcpToolId, enable));
    }
}

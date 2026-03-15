/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
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
class EmbeddedMcpServerFacadeImpl implements EmbeddedMcpServerFacade {

    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerFacade mcpServerFacade;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerFacadeImpl(
        McpComponentService mcpComponentService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpServerFacade mcpServerFacade, McpToolService mcpToolService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpServerFacade = mcpServerFacade;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public void deleteEmbeddedMcpServer(long mcpServerId) {
        for (var mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            for (var mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())) {
                mcpIntegrationInstanceToolService.deleteByMcpToolId(mcpTool.getId());
            }
        }

        mcpServerFacade.deleteMcpServer(mcpServerId);
    }
}

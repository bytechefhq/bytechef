/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
class EmbeddedMcpReader {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    EmbeddedMcpReader(
        McpComponentService mcpComponentService, McpServerService mcpServerService,
        McpToolService mcpToolService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
    }

    McpServer getMcpServer(long mcpServerId) {
        return mcpServerService.fetchMcpServer(mcpServerId)
            .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
            .orElseThrow(() -> new IllegalArgumentException("Embedded MCP server not found: " + mcpServerId));
    }

    Optional<McpTool> fetchMcpTool(long mcpToolId) {
        Optional<Long> mcpComponentId = mcpToolService.fetchMcpToolMcpComponentId(mcpToolId);

        boolean embedded = mcpComponentId.flatMap(mcpComponentService::fetchMcpComponentMcpServerId)
            .flatMap(mcpServerService::fetchMcpServer)
            .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
            .isPresent();

        if (!embedded) {
            return Optional.empty();
        }

        List<McpTool> mcpTools = mcpToolService.getMcpComponentMcpTools(mcpComponentId.get());

        return mcpTools.stream()
            .filter(mcpTool -> Objects.equals(mcpTool.getId(), mcpToolId))
            .findFirst();
    }
}

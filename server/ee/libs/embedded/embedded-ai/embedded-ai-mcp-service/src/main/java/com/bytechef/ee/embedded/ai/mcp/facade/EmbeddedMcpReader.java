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
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private final McpComponentRepository mcpComponentRepository;
    private final McpServerRepository mcpServerRepository;
    private final McpToolRepository mcpToolRepository;

    @SuppressFBWarnings("EI")
    EmbeddedMcpReader(
        McpComponentRepository mcpComponentRepository, McpServerRepository mcpServerRepository,
        McpToolRepository mcpToolRepository) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.mcpToolRepository = mcpToolRepository;
    }

    McpServer getMcpServer(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
            .orElseThrow(() -> new IllegalArgumentException("Embedded MCP server not found: " + mcpServerId));
    }

    Optional<McpTool> fetchMcpTool(long mcpToolId) {
        return mcpToolRepository.findById(mcpToolId)
            .filter(
                mcpTool -> mcpComponentRepository.findById(mcpTool.getMcpComponentId())
                    .flatMap(mcpComponent -> mcpServerRepository.findById(mcpComponent.getMcpServerId()))
                    .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
                    .isPresent());
    }
}

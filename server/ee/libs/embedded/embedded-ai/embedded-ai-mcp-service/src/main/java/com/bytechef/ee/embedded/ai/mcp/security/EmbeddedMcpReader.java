/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
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
public class EmbeddedMcpReader {

    private final McpServerRepository mcpServerRepository;
    private final McpToolRepository mcpToolRepository;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpReader(McpServerRepository mcpServerRepository, McpToolRepository mcpToolRepository) {
        this.mcpServerRepository = mcpServerRepository;
        this.mcpToolRepository = mcpToolRepository;
    }

    public McpServer getMcpServer(long mcpServerId) {
        return mcpServerRepository.findById(mcpServerId)
            .orElseThrow(() -> new IllegalArgumentException("MCP server not found: " + mcpServerId));
    }

    public Optional<McpTool> fetchMcpTool(long mcpToolId) {
        return mcpToolRepository.findById(mcpToolId);
    }
}

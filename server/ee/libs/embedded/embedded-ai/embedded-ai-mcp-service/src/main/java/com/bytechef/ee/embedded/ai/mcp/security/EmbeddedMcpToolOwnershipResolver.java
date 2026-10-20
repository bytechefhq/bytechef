/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedMcpToolOwnershipResolver implements ResourceOwnershipResolver {

    private final McpComponentService mcpComponentService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpToolOwnershipResolver(
        McpComponentService mcpComponentService, McpServerService mcpServerService,
        McpToolService mcpToolService) {

        this.mcpComponentService = mcpComponentService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public String resourceType() {
        return "McpTool";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpToolService.fetchMcpToolMcpComponentId(id)
            .flatMap(mcpComponentService::fetchMcpComponentMcpServerId)
            .flatMap(mcpServerService::fetchMcpServer)
            .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
            .map(mcpServer -> ResourceOwner.ofEmbedded())
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}

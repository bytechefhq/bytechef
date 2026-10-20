/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceToolRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class McpIntegrationInstanceToolEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpComponentRepository mcpComponentRepository;
    private final McpServerRepository mcpServerRepository;
    private final McpToolRepository mcpToolRepository;
    private final McpIntegrationInstanceToolRepository mcpIntegrationInstanceToolRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceToolEnvironmentResolver(
        McpComponentRepository mcpComponentRepository, McpServerRepository mcpServerRepository,
        McpToolRepository mcpToolRepository,
        McpIntegrationInstanceToolRepository mcpIntegrationInstanceToolRepository) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.mcpToolRepository = mcpToolRepository;
        this.mcpIntegrationInstanceToolRepository = mcpIntegrationInstanceToolRepository;
    }

    @Override
    public String resourceType() {
        return "McpIntegrationInstanceTool";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpIntegrationInstanceToolRepository.findById(number.longValue())
            .map(McpIntegrationInstanceTool::getMcpToolId)
            .flatMap(mcpToolRepository::findById)
            .map(McpTool::getMcpComponentId)
            .flatMap(mcpComponentRepository::findById)
            .map(McpComponent::getMcpServerId)
            .flatMap(mcpServerRepository::findById)
            .map(McpServer::getEnvironment);
    }
}

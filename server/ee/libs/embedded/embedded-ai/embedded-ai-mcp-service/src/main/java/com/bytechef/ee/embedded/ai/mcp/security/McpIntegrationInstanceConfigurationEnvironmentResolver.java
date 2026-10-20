/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
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
public class McpIntegrationInstanceConfigurationEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpIntegrationInstanceConfigurationRepository mcpIntegrationInstanceConfigurationRepository;
    private final McpServerService mcpServerService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationEnvironmentResolver(
        McpIntegrationInstanceConfigurationRepository mcpIntegrationInstanceConfigurationRepository,
        McpServerService mcpServerService) {

        this.mcpIntegrationInstanceConfigurationRepository = mcpIntegrationInstanceConfigurationRepository;
        this.mcpServerService = mcpServerService;
    }

    @Override
    public String resourceType() {
        return "McpIntegrationInstanceConfiguration";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpIntegrationInstanceConfigurationRepository.findById(number.longValue())
            .map(McpIntegrationInstanceConfiguration::getMcpServerId)
            .flatMap(mcpServerService::fetchMcpServer)
            .map(McpServer::getEnvironment);
    }
}

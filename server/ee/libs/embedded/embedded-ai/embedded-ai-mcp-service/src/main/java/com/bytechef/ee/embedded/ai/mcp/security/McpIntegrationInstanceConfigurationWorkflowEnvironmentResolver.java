/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
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
public class McpIntegrationInstanceConfigurationWorkflowEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpIntegrationInstanceConfigurationRepository mcpIntegrationInstanceConfigurationRepository;
    private final McpIntegrationInstanceConfigurationWorkflowRepository mcpIntegrationInstanceConfigurationWorkflowRepository;
    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationWorkflowEnvironmentResolver(
        McpIntegrationInstanceConfigurationRepository mcpIntegrationInstanceConfigurationRepository,
        McpIntegrationInstanceConfigurationWorkflowRepository mcpIntegrationInstanceConfigurationWorkflowRepository,
        McpServerRepository mcpServerRepository) {

        this.mcpIntegrationInstanceConfigurationRepository = mcpIntegrationInstanceConfigurationRepository;
        this.mcpIntegrationInstanceConfigurationWorkflowRepository =
            mcpIntegrationInstanceConfigurationWorkflowRepository;
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public String resourceType() {
        return "McpIntegrationInstanceConfigurationWorkflow";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpIntegrationInstanceConfigurationWorkflowRepository.findById(number.longValue())
            .map(McpIntegrationInstanceConfigurationWorkflow::getMcpIntegrationInstanceConfigurationId)
            .flatMap(mcpIntegrationInstanceConfigurationRepository::findById)
            .map(McpIntegrationInstanceConfiguration::getMcpServerId)
            .flatMap(mcpServerRepository::findById)
            .map(McpServer::getEnvironment);
    }
}

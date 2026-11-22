/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.repository.McpAutomationProjectConfigurationRepository;
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
public class McpAutomationProjectConfigurationEnvironmentResolver implements ResourceEnvironmentResolver {

    private final McpAutomationProjectConfigurationRepository mcpAutomationProjectConfigurationRepository;
    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public McpAutomationProjectConfigurationEnvironmentResolver(
        McpAutomationProjectConfigurationRepository mcpAutomationProjectConfigurationRepository,
        McpServerRepository mcpServerRepository) {

        this.mcpAutomationProjectConfigurationRepository = mcpAutomationProjectConfigurationRepository;
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public String resourceType() {
        return "McpAutomationProjectConfiguration";
    }

    @Override
    public Optional<Environment> fetchEnvironment(Serializable id) {
        if (!(id instanceof Number number)) {
            return Optional.empty();
        }

        return mcpAutomationProjectConfigurationRepository.findById(number.longValue())
            .map(McpAutomationProjectConfiguration::getMcpServerId)
            .flatMap(mcpServerRepository::findById)
            .map(McpServer::getEnvironment);
    }
}

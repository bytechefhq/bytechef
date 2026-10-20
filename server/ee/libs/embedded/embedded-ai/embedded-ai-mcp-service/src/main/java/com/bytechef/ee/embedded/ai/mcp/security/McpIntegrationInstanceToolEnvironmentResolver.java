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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
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

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final McpIntegrationInstanceToolRepository mcpIntegrationInstanceToolRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceToolEnvironmentResolver(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService,
        McpIntegrationInstanceToolRepository mcpIntegrationInstanceToolRepository) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
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
            .map(McpIntegrationInstanceTool::getIntegrationInstanceId)
            .flatMap(integrationInstanceId -> integrationInstanceService
                .getIntegrationInstances(List.of(integrationInstanceId))
                .stream()
                .findFirst())
            .map(IntegrationInstance::getIntegrationInstanceConfigurationId)
            .flatMap(configurationId -> integrationInstanceConfigurationService
                .getIntegrationInstanceConfigurations(List.of(configurationId))
                .stream()
                .findFirst())
            .map(IntegrationInstanceConfiguration::getEnvironment);
    }
}

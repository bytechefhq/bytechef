/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.remote.client.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
public class RemoteMcpIntegrationInstanceConfigurationServiceClient
    implements McpIntegrationInstanceConfigurationService {

    @Override
    public McpIntegrationInstanceConfiguration
        create(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long mcpIntegrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByIntegrationInstanceConfigurationId(long integrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationInstanceConfiguration>
        fetchMcpIntegrationInstanceConfiguration(long mcpIntegrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceConfiguration>
        getMcpIntegrationInstanceConfigurationsByIntegrationId(long integrationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceConfiguration>
        getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
            long integrationInstanceConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceConfiguration> getMcpIntegrationInstanceConfigurations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceConfiguration>
        getMcpServerMcpIntegrationInstanceConfigurations(long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public McpIntegrationInstanceConfiguration
        update(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        throw new UnsupportedOperationException();
    }
}

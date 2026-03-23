/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.repository.McpIntegrationInstanceConfigurationRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class McpIntegrationInstanceConfigurationServiceImpl implements McpIntegrationInstanceConfigurationService {

    private final McpIntegrationInstanceConfigurationRepository mcpIntegrationInstanceConfigurationRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationServiceImpl(
        McpIntegrationInstanceConfigurationRepository mcpIntegrationInstanceConfigurationRepository) {
        this.mcpIntegrationInstanceConfigurationRepository = mcpIntegrationInstanceConfigurationRepository;
    }

    @Override
    public McpIntegrationInstanceConfiguration
        create(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        return mcpIntegrationInstanceConfigurationRepository.save(mcpIntegrationInstanceConfiguration);
    }

    @Override
    public void delete(long mcpIntegrationInstanceConfigurationId) {
        mcpIntegrationInstanceConfigurationRepository.deleteById(mcpIntegrationInstanceConfigurationId);
    }

    @Override
    public void deleteByIntegrationInstanceConfigurationId(long integrationInstanceConfigurationId) {
        mcpIntegrationInstanceConfigurationRepository.deleteAllByIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationInstanceConfiguration>
        fetchMcpIntegrationInstanceConfiguration(long mcpIntegrationInstanceConfigurationId) {
        return mcpIntegrationInstanceConfigurationRepository.findById(mcpIntegrationInstanceConfigurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceConfiguration>
        getMcpIntegrationInstanceConfigurationsByIntegrationId(long integrationId) {
        return mcpIntegrationInstanceConfigurationRepository.findAllByIntegrationId(integrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceConfiguration>
        getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
            long integrationInstanceConfigurationId) {

        return mcpIntegrationInstanceConfigurationRepository.findAllByIntegrationInstanceConfigurationId(
            integrationInstanceConfigurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceConfiguration> getMcpIntegrationInstanceConfigurations() {
        return mcpIntegrationInstanceConfigurationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceConfiguration>
        getMcpServerMcpIntegrationInstanceConfigurations(long mcpServerId) {
        return mcpIntegrationInstanceConfigurationRepository.findAllByMcpServerId(mcpServerId);
    }

    @Override
    public McpIntegrationInstanceConfiguration
        update(McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {
        McpIntegrationInstanceConfiguration currentMcpIntegrationInstanceConfiguration =
            OptionalUtils.get(
                mcpIntegrationInstanceConfigurationRepository.findById(mcpIntegrationInstanceConfiguration.getId()));

        currentMcpIntegrationInstanceConfiguration.setIntegrationInstanceConfigurationId(
            mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());
        currentMcpIntegrationInstanceConfiguration.setMcpServerId(mcpIntegrationInstanceConfiguration.getMcpServerId());
        currentMcpIntegrationInstanceConfiguration.setVersion(mcpIntegrationInstanceConfiguration.getVersion());

        return mcpIntegrationInstanceConfigurationRepository.save(currentMcpIntegrationInstanceConfiguration);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.repository.McpAutomationProjectConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class McpAutomationProjectConfigurationServiceImpl implements McpAutomationProjectConfigurationService {

    private final McpAutomationProjectConfigurationRepository mcpAutomationProjectConfigurationRepository;

    @SuppressFBWarnings("EI")
    public McpAutomationProjectConfigurationServiceImpl(
        McpAutomationProjectConfigurationRepository mcpAutomationProjectConfigurationRepository) {

        this.mcpAutomationProjectConfigurationRepository = mcpAutomationProjectConfigurationRepository;
    }

    @Override
    public McpAutomationProjectConfiguration
        create(McpAutomationProjectConfiguration mcpAutomationProjectConfiguration) {
        return mcpAutomationProjectConfigurationRepository.save(mcpAutomationProjectConfiguration);
    }

    @Override
    public void delete(long id) {
        mcpAutomationProjectConfigurationRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpAutomationProjectConfiguration> fetch(long id) {
        return mcpAutomationProjectConfigurationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpAutomationProjectConfiguration>
        getAutomationProjectConfigurationMcpAutomationProjectConfigurations(long automationProjectConfigurationId) {

        return mcpAutomationProjectConfigurationRepository.findAllByAutomationProjectConfigurationId(
            automationProjectConfigurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpAutomationProjectConfiguration> getMcpServerMcpAutomationProjectConfigurations(long mcpServerId) {
        return mcpAutomationProjectConfigurationRepository.findAllByMcpServerId(mcpServerId);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import com.bytechef.ee.embedded.mcp.repository.McpIntegrationRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpIntegrationService} interface.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
public class McpIntegrationServiceImpl implements McpIntegrationService {

    private final McpIntegrationRepository mcpIntegrationRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationServiceImpl(McpIntegrationRepository mcpIntegrationRepository) {
        this.mcpIntegrationRepository = mcpIntegrationRepository;
    }

    @Override
    public McpIntegration create(McpIntegration mcpIntegration) {
        return mcpIntegrationRepository.save(mcpIntegration);
    }

    @Override
    public void delete(long mcpIntegrationId) {
        mcpIntegrationRepository.deleteById(mcpIntegrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegration> fetchMcpIntegration(long mcpIntegrationId) {
        return mcpIntegrationRepository.findById(mcpIntegrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegration> getMcpIntegrations() {
        return mcpIntegrationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegration> getMcpServerMcpIntegrations(long mcpServerId) {
        return mcpIntegrationRepository.findAllByMcpServerId(mcpServerId);
    }

    @Override
    public McpIntegration update(McpIntegration mcpIntegration) {
        McpIntegration currentMcpIntegration =
            OptionalUtils.get(mcpIntegrationRepository.findById(mcpIntegration.getId()));

        currentMcpIntegration.setIntegrationInstanceConfigurationId(
            mcpIntegration.getIntegrationInstanceConfigurationId());
        currentMcpIntegration.setMcpServerId(mcpIntegration.getMcpServerId());
        currentMcpIntegration.setVersion(mcpIntegration.getVersion());

        return mcpIntegrationRepository.save(currentMcpIntegration);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.mcp.repository.McpIntegrationInstanceToolRepository;
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
public class McpIntegrationInstanceToolServiceImpl implements McpIntegrationInstanceToolService {

    private final McpIntegrationInstanceToolRepository mcpIntegrationInstanceToolRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceToolServiceImpl(
        McpIntegrationInstanceToolRepository mcpIntegrationInstanceToolRepository) {

        this.mcpIntegrationInstanceToolRepository = mcpIntegrationInstanceToolRepository;
    }

    @Override
    public void deleteByIntegrationInstanceId(long integrationInstanceId) {
        mcpIntegrationInstanceToolRepository.deleteByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    public void deleteByMcpToolId(long mcpToolId) {
        mcpIntegrationInstanceToolRepository.deleteByMcpToolId(mcpToolId);
    }

    @Override
    public McpIntegrationInstanceTool createMcpIntegrationInstanceTool(
        long integrationInstanceId, long mcpToolId, boolean enabled) {

        McpIntegrationInstanceTool mcpIntegrationInstanceTool =
            new McpIntegrationInstanceTool(integrationInstanceId, mcpToolId, enabled);

        return mcpIntegrationInstanceToolRepository.save(mcpIntegrationInstanceTool);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationInstanceTool> fetchMcpIntegrationInstanceTool(
        long integrationInstanceId, long mcpToolId) {

        return mcpIntegrationInstanceToolRepository.findByIntegrationInstanceIdAndMcpToolId(
            integrationInstanceId, mcpToolId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceTool> getMcpIntegrationInstanceTools(long integrationInstanceId) {
        return mcpIntegrationInstanceToolRepository.findAllByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        McpIntegrationInstanceTool mcpIntegrationInstanceTool =
            mcpIntegrationInstanceToolRepository.findById(id)
                .orElseThrow(
                    () -> new IllegalArgumentException("McpIntegrationInstanceTool not found: " + id));

        mcpIntegrationInstanceTool.setEnabled(enabled);

        mcpIntegrationInstanceToolRepository.save(mcpIntegrationInstanceTool);
    }
}

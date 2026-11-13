/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceWorkflowRepository;
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
public class McpIntegrationInstanceWorkflowServiceImpl implements McpIntegrationInstanceWorkflowService {

    private final McpIntegrationInstanceWorkflowRepository mcpIntegrationInstanceWorkflowRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceWorkflowServiceImpl(
        McpIntegrationInstanceWorkflowRepository mcpIntegrationInstanceWorkflowRepository) {

        this.mcpIntegrationInstanceWorkflowRepository = mcpIntegrationInstanceWorkflowRepository;
    }

    @Override
    public McpIntegrationInstanceWorkflow createMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId, boolean enabled) {

        return mcpIntegrationInstanceWorkflowRepository.save(
            new McpIntegrationInstanceWorkflow(
                integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, enabled));
    }

    @Override
    public void delete(long id) {
        mcpIntegrationInstanceWorkflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationInstanceWorkflow> fetchMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId) {

        return mcpIntegrationInstanceWorkflowRepository
            .findByIntegrationInstanceIdAndMcpIntegrationInstanceConfigurationWorkflowId(
                integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getIntegrationInstanceIdsByMcpServerId(long mcpServerId) {
        return mcpIntegrationInstanceWorkflowRepository.findIntegrationInstanceIdsByMcpServerId(mcpServerId);
    }

    @Override
    @Transactional(readOnly = true)
    public McpIntegrationInstanceWorkflow getMcpIntegrationInstanceWorkflow(long id) {
        return mcpIntegrationInstanceWorkflowRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("McpIntegrationInstanceWorkflow not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceWorkflow> getMcpIntegrationInstanceWorkflows(long integrationInstanceId) {
        return mcpIntegrationInstanceWorkflowRepository.findAllByIntegrationInstanceId(integrationInstanceId);
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow = getMcpIntegrationInstanceWorkflow(id);

        mcpIntegrationInstanceWorkflow.setEnabled(enabled);

        mcpIntegrationInstanceWorkflowRepository.save(mcpIntegrationInstanceWorkflow);
    }
}

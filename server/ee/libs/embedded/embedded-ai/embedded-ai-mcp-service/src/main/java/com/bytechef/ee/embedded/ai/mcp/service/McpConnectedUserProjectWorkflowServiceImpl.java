/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.repository.McpConnectedUserProjectWorkflowRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class McpConnectedUserProjectWorkflowServiceImpl implements McpConnectedUserProjectWorkflowService {

    private final McpConnectedUserProjectWorkflowRepository mcpConnectedUserProjectWorkflowRepository;

    @SuppressFBWarnings("EI")
    public McpConnectedUserProjectWorkflowServiceImpl(
        McpConnectedUserProjectWorkflowRepository mcpConnectedUserProjectWorkflowRepository) {

        this.mcpConnectedUserProjectWorkflowRepository = mcpConnectedUserProjectWorkflowRepository;
    }

    @Override
    public McpConnectedUserProjectWorkflow create(
        long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId, boolean enabled) {

        return mcpConnectedUserProjectWorkflowRepository.save(
            new McpConnectedUserProjectWorkflow(
                connectedUserProjectWorkflowId, mcpAutomationProjectConfigurationWorkflowId, enabled));
    }

    @Override
    public void delete(long id) {
        mcpConnectedUserProjectWorkflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpConnectedUserProjectWorkflow> fetch(long id) {
        return mcpConnectedUserProjectWorkflowRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpConnectedUserProjectWorkflow> fetch(
        long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId) {

        return mcpConnectedUserProjectWorkflowRepository
            .findByConnectedUserProjectWorkflowIdAndMcpAutomationProjectConfigurationWorkflowId(
                connectedUserProjectWorkflowId, mcpAutomationProjectConfigurationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getConnectedUserIdsByMcpServerId(long mcpServerId) {
        return new HashSet<>(mcpConnectedUserProjectWorkflowRepository.findConnectedUserIdsByMcpServerId(mcpServerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpConnectedUserProjectWorkflow> getConnectedUserMcpConnectedUserProjectWorkflows(
        long connectedUserId) {

        return mcpConnectedUserProjectWorkflowRepository.findAllByConnectedUserId(connectedUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpConnectedUserProjectWorkflow> getMcpServerMcpConnectedUserProjectWorkflows(long mcpServerId) {
        return mcpConnectedUserProjectWorkflowRepository.findAllByMcpServerId(mcpServerId);
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        McpConnectedUserProjectWorkflow mcpConnectedUserProjectWorkflow =
            mcpConnectedUserProjectWorkflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("McpConnectedUserProjectWorkflow not found: " + id));

        mcpConnectedUserProjectWorkflow.setEnabled(enabled);

        mcpConnectedUserProjectWorkflowRepository.save(mcpConnectedUserProjectWorkflow);
    }
}

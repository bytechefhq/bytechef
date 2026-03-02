/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.repository.McpIntegrationWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpIntegrationWorkflowService} interface.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
public class McpIntegrationWorkflowServiceImpl implements McpIntegrationWorkflowService {

    private final McpIntegrationWorkflowRepository mcpIntegrationWorkflowRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationWorkflowServiceImpl(McpIntegrationWorkflowRepository mcpIntegrationWorkflowRepository) {
        this.mcpIntegrationWorkflowRepository = mcpIntegrationWorkflowRepository;
    }

    @Override
    public McpIntegrationWorkflow create(McpIntegrationWorkflow mcpIntegrationWorkflow) {
        return mcpIntegrationWorkflowRepository.save(mcpIntegrationWorkflow);
    }

    @Override
    public McpIntegrationWorkflow create(Long mcpIntegrationId, Long integrationInstanceConfigurationWorkflowId) {
        McpIntegrationWorkflow mcpIntegrationWorkflow = new McpIntegrationWorkflow(
            mcpIntegrationId, integrationInstanceConfigurationWorkflowId);

        return create(mcpIntegrationWorkflow);
    }

    @Override
    public void delete(long mcpIntegrationWorkflowId) {
        mcpIntegrationWorkflowRepository.deleteById(mcpIntegrationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationWorkflow> fetchMcpIntegrationWorkflow(long mcpIntegrationWorkflowId) {
        return mcpIntegrationWorkflowRepository.findById(mcpIntegrationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationWorkflow> getMcpIntegrationMcpIntegrationWorkflows(Long mcpIntegrationId) {
        return mcpIntegrationWorkflowRepository.findAllByMcpIntegrationId(mcpIntegrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationWorkflow> getMcpIntegrationWorkflows() {
        return mcpIntegrationWorkflowRepository.findAll();
    }

    @Override
    public McpIntegrationWorkflow update(McpIntegrationWorkflow mcpIntegrationWorkflow) {
        McpIntegrationWorkflow currentMcpIntegrationWorkflow =
            OptionalUtils.get(mcpIntegrationWorkflowRepository.findById(mcpIntegrationWorkflow.getId()));

        currentMcpIntegrationWorkflow.setMcpIntegrationId(mcpIntegrationWorkflow.getMcpIntegrationId());
        currentMcpIntegrationWorkflow.setIntegrationInstanceConfigurationWorkflowId(
            mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
        currentMcpIntegrationWorkflow.setVersion(mcpIntegrationWorkflow.getVersion());

        return mcpIntegrationWorkflowRepository.save(currentMcpIntegrationWorkflow);
    }

    @Override
    public McpIntegrationWorkflow update(
        long id, Long mcpIntegrationId, Long integrationInstanceConfigurationWorkflowId) {

        McpIntegrationWorkflow existingMcpIntegrationWorkflow = fetchMcpIntegrationWorkflow(id)
            .orElseThrow(
                () -> new IllegalArgumentException("McpIntegrationWorkflow not found with id: " + id));

        if (mcpIntegrationId != null) {
            existingMcpIntegrationWorkflow.setMcpIntegrationId(mcpIntegrationId);
        }

        if (integrationInstanceConfigurationWorkflowId != null) {
            existingMcpIntegrationWorkflow.setIntegrationInstanceConfigurationWorkflowId(
                integrationInstanceConfigurationWorkflowId);
        }

        return update(existingMcpIntegrationWorkflow);
    }

    @Override
    public McpIntegrationWorkflow updateParameters(long id, Map<String, ?> parameters) {
        McpIntegrationWorkflow existingMcpIntegrationWorkflow = fetchMcpIntegrationWorkflow(id)
            .orElseThrow(
                () -> new IllegalArgumentException("McpIntegrationWorkflow not found with id: " + id));

        existingMcpIntegrationWorkflow.setParameters(parameters);

        return mcpIntegrationWorkflowRepository.save(existingMcpIntegrationWorkflow);
    }
}

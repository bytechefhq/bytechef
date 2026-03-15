/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.repository.McpIntegrationInstanceConfigurationWorkflowRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link McpIntegrationInstanceConfigurationWorkflowService} interface.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
public class McpIntegrationInstanceConfigurationWorkflowServiceImpl
    implements McpIntegrationInstanceConfigurationWorkflowService {

    private final McpIntegrationInstanceConfigurationWorkflowRepository mcpIntegrationInstanceConfigurationWorkflowRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationWorkflowServiceImpl(
        McpIntegrationInstanceConfigurationWorkflowRepository mcpIntegrationInstanceConfigurationWorkflowRepository) {
        this.mcpIntegrationInstanceConfigurationWorkflowRepository =
            mcpIntegrationInstanceConfigurationWorkflowRepository;
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow
        create(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {
        return mcpIntegrationInstanceConfigurationWorkflowRepository.save(mcpIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow
        create(Long mcpIntegrationInstanceConfigurationId, Long integrationInstanceConfigurationWorkflowId) {
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            new McpIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);

        return create(mcpIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    public void delete(long mcpIntegrationInstanceConfigurationWorkflowId) {
        mcpIntegrationInstanceConfigurationWorkflowRepository.deleteById(mcpIntegrationInstanceConfigurationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId) {
        return mcpIntegrationInstanceConfigurationWorkflowRepository
            .findById(mcpIntegrationInstanceConfigurationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflowByIntegrationInstanceConfigurationWorkflowId(
            long integrationInstanceConfigurationWorkflowId) {

        return mcpIntegrationInstanceConfigurationWorkflowRepository
            .findByIntegrationInstanceConfigurationWorkflowId(integrationInstanceConfigurationWorkflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflowByWorkflowId(String workflowId) {
        return mcpIntegrationInstanceConfigurationWorkflowRepository.findByWorkflowId(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceConfigurationWorkflow>
        getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
            Long mcpIntegrationInstanceConfigurationId) {
        return mcpIntegrationInstanceConfigurationWorkflowRepository
            .findAllByMcpIntegrationInstanceConfigurationId(mcpIntegrationInstanceConfigurationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows() {
        return mcpIntegrationInstanceConfigurationWorkflowRepository.findAll();
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow
        update(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {
        McpIntegrationInstanceConfigurationWorkflow currentMcpIntegrationInstanceConfigurationWorkflow =
            OptionalUtils.get(mcpIntegrationInstanceConfigurationWorkflowRepository
                .findById(mcpIntegrationInstanceConfigurationWorkflow.getId()));

        currentMcpIntegrationInstanceConfigurationWorkflow.setMcpIntegrationInstanceConfigurationId(
            mcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId());
        currentMcpIntegrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationWorkflowId(
            mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
        currentMcpIntegrationInstanceConfigurationWorkflow
            .setVersion(mcpIntegrationInstanceConfigurationWorkflow.getVersion());

        return mcpIntegrationInstanceConfigurationWorkflowRepository
            .save(currentMcpIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow update(
        long id, Long mcpIntegrationInstanceConfigurationId, Long integrationInstanceConfigurationWorkflowId) {

        McpIntegrationInstanceConfigurationWorkflow existingMcpIntegrationInstanceConfigurationWorkflow =
            fetchMcpIntegrationInstanceConfigurationWorkflow(id)
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "McpIntegrationInstanceConfigurationWorkflow not found with id: " + id));

        if (mcpIntegrationInstanceConfigurationId != null) {
            existingMcpIntegrationInstanceConfigurationWorkflow
                .setMcpIntegrationInstanceConfigurationId(mcpIntegrationInstanceConfigurationId);
        }

        if (integrationInstanceConfigurationWorkflowId != null) {
            existingMcpIntegrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationWorkflowId(
                integrationInstanceConfigurationWorkflowId);
        }

        return update(existingMcpIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow updateParameters(long id, Map<String, ?> parameters) {
        McpIntegrationInstanceConfigurationWorkflow existingMcpIntegrationInstanceConfigurationWorkflow =
            fetchMcpIntegrationInstanceConfigurationWorkflow(id)
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "McpIntegrationInstanceConfigurationWorkflow not found with id: " + id));

        existingMcpIntegrationInstanceConfigurationWorkflow.setParameters(parameters);

        return mcpIntegrationInstanceConfigurationWorkflowRepository
            .save(existingMcpIntegrationInstanceConfigurationWorkflow);
    }

    @Override
    public void deleteByIntegrationInstanceConfigurationWorkflowId(
        long integrationInstanceConfigurationWorkflowId) {

        mcpIntegrationInstanceConfigurationWorkflowRepository.deleteByIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceConfigurationWorkflowId);
    }
}

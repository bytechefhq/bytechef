/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.repository.McpAutomationProjectConfigurationWorkflowRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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
public class McpAutomationProjectConfigurationWorkflowServiceImpl
    implements McpAutomationProjectConfigurationWorkflowService {

    private final McpAutomationProjectConfigurationWorkflowRepository mcpAutomationProjectConfigurationWorkflowRepository;

    @SuppressFBWarnings("EI")
    public McpAutomationProjectConfigurationWorkflowServiceImpl(
        McpAutomationProjectConfigurationWorkflowRepository mcpAutomationProjectConfigurationWorkflowRepository) {

        this.mcpAutomationProjectConfigurationWorkflowRepository = mcpAutomationProjectConfigurationWorkflowRepository;
    }

    @Override
    public McpAutomationProjectConfigurationWorkflow create(
        long mcpAutomationProjectConfigurationId, String workflowUuid) {

        return mcpAutomationProjectConfigurationWorkflowRepository.save(
            new McpAutomationProjectConfigurationWorkflow(mcpAutomationProjectConfigurationId, workflowUuid));
    }

    @Override
    public void delete(long id) {
        mcpAutomationProjectConfigurationWorkflowRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpAutomationProjectConfigurationWorkflow> fetch(long id) {
        return mcpAutomationProjectConfigurationWorkflowRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpAutomationProjectConfigurationWorkflow> getMcpAutomationProjectConfigurationWorkflows(
        long mcpAutomationProjectConfigurationId) {

        return mcpAutomationProjectConfigurationWorkflowRepository.findAllByMcpAutomationProjectConfigurationId(
            mcpAutomationProjectConfigurationId);
    }

    @Override
    public McpAutomationProjectConfigurationWorkflow updateEnabled(long id, boolean enabled) {
        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow = getWorkflow(id);

        mcpAutomationProjectConfigurationWorkflow.setEnabled(enabled);

        return mcpAutomationProjectConfigurationWorkflowRepository.save(mcpAutomationProjectConfigurationWorkflow);
    }

    @Override
    public McpAutomationProjectConfigurationWorkflow updateParameters(long id, Map<String, ?> parameters) {
        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow = getWorkflow(id);

        mcpAutomationProjectConfigurationWorkflow.setParameters(parameters);

        return mcpAutomationProjectConfigurationWorkflowRepository.save(mcpAutomationProjectConfigurationWorkflow);
    }

    private McpAutomationProjectConfigurationWorkflow getWorkflow(long id) {
        return mcpAutomationProjectConfigurationWorkflowRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("McpAutomationProjectConfigurationWorkflow not found: " + id));
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.remote.client.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpAutomationProjectConfigurationWorkflowServiceClient
    implements McpAutomationProjectConfigurationWorkflowService {

    @Override
    public McpAutomationProjectConfigurationWorkflow create(
        long mcpAutomationProjectConfigurationId, String workflowUuid) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpAutomationProjectConfigurationWorkflow> fetch(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpAutomationProjectConfigurationWorkflow> getMcpAutomationProjectConfigurationWorkflows(
        long mcpAutomationProjectConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public McpAutomationProjectConfigurationWorkflow updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public McpAutomationProjectConfigurationWorkflow updateParameters(long id, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }
}

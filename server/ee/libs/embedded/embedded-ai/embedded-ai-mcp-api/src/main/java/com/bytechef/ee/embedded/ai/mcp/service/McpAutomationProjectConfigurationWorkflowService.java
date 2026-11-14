/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpAutomationProjectConfigurationWorkflowService {

    McpAutomationProjectConfigurationWorkflow create(long mcpAutomationProjectConfigurationId, String workflowUuid);

    void delete(long id);

    Optional<McpAutomationProjectConfigurationWorkflow> fetch(long id);

    List<McpAutomationProjectConfigurationWorkflow>
        getMcpAutomationProjectConfigurationWorkflows(long mcpAutomationProjectConfigurationId);

    McpAutomationProjectConfigurationWorkflow updateEnabled(long id, boolean enabled);

    McpAutomationProjectConfigurationWorkflow updateParameters(long id, Map<String, ?> parameters);
}

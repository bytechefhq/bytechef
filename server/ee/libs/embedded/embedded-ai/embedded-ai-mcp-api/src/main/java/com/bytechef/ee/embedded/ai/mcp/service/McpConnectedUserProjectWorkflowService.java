/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpConnectedUserProjectWorkflowService {

    McpConnectedUserProjectWorkflow create(
        long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId, boolean enabled);

    void delete(long id);

    Optional<McpConnectedUserProjectWorkflow> fetch(long id);

    Optional<McpConnectedUserProjectWorkflow> fetch(
        long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId);

    Set<Long> getConnectedUserIdsByMcpServerId(long mcpServerId);

    List<McpConnectedUserProjectWorkflow> getConnectedUserMcpConnectedUserProjectWorkflows(long connectedUserId);

    List<McpConnectedUserProjectWorkflow> getMcpServerMcpConnectedUserProjectWorkflows(long mcpServerId);

    void updateEnabled(long id, boolean enabled);
}

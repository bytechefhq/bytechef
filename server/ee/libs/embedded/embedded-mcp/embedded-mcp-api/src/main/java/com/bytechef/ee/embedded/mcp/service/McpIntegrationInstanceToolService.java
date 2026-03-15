/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceToolService {

    McpIntegrationInstanceTool createMcpIntegrationInstanceTool(
        long integrationInstanceId, long mcpToolId, boolean enabled);

    void deleteByIntegrationInstanceId(long integrationInstanceId);

    void deleteByMcpToolId(long mcpToolId);

    Optional<McpIntegrationInstanceTool> fetchMcpIntegrationInstanceTool(
        long integrationInstanceId, long mcpToolId);

    List<McpIntegrationInstanceTool> getMcpIntegrationInstanceTools(long integrationInstanceId);

    void updateEnabled(Long id, boolean enabled);
}

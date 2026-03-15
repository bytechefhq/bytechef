/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.remote.client.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpIntegrationInstanceToolServiceClient implements McpIntegrationInstanceToolService {

    @Override
    public McpIntegrationInstanceTool createMcpIntegrationInstanceTool(
        long integrationInstanceId, long mcpToolId, boolean enabled) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByIntegrationInstanceId(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByMcpToolId(long mcpToolId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationInstanceTool> fetchMcpIntegrationInstanceTool(
        long integrationInstanceId, long mcpToolId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceTool> getMcpIntegrationInstanceTools(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}

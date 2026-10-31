/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.remote.client.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationComponent;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
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
public class RemoteMcpIntegrationComponentServiceClient implements McpIntegrationComponentService {

    @Override
    public McpIntegrationComponent create(long mcpComponentId, long integrationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationComponent> fetchMcpIntegrationComponent(long mcpComponentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationComponent> getIntegrationMcpIntegrationComponents(long integrationId) {
        throw new UnsupportedOperationException();
    }
}

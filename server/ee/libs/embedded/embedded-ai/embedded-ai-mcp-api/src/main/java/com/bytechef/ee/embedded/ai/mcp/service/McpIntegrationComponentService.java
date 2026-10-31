/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationComponent;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationComponentService {

    McpIntegrationComponent create(long mcpComponentId, long integrationId);

    Optional<McpIntegrationComponent> fetchMcpIntegrationComponent(long mcpComponentId);

    List<McpIntegrationComponent> getIntegrationMcpIntegrationComponents(long integrationId);
}

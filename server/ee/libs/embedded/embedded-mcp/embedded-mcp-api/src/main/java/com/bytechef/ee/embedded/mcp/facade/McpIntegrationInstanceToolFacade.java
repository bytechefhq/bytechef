/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceToolFacade {

    void enableMcpIntegrationInstanceTool(long integrationInstanceId, long mcpToolId, boolean enable);
}

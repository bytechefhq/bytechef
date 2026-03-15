/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.remote.client.facade;

import com.bytechef.ee.embedded.mcp.facade.McpIntegrationInstanceToolFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpIntegrationInstanceToolFacadeClient implements McpIntegrationInstanceToolFacade {

    @Override
    public void enableMcpIntegrationInstanceTool(long integrationInstanceId, long mcpToolId, boolean enable) {
        throw new UnsupportedOperationException();
    }
}

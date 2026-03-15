/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.remote.client.facade;

import com.bytechef.ee.embedded.mcp.facade.McpIntegrationInstanceWorkflowFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpIntegrationInstanceWorkflowFacadeClient implements McpIntegrationInstanceWorkflowFacade {

    @Override
    public void enableMcpIntegrationInstanceWorkflow(long integrationInstanceId, String workflowUuid, boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowUuid, Map<String, Object> inputs) {

        throw new UnsupportedOperationException();
    }
}

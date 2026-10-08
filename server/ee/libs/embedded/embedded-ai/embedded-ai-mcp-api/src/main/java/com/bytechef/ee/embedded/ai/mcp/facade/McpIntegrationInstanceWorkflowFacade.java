/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceWorkflowFacade {

    void enableMcpIntegrationInstanceWorkflow(
        String externalUserId, long integrationInstanceId, String workflowUuid, boolean enable);

    void updateMcpIntegrationInstanceWorkflow(
        String externalUserId, long integrationInstanceId, String workflowUuid, Map<String, Object> inputs);
}

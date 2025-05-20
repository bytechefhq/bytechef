/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.workflow.coordinator;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class AbstractDispatcherPreSendProcessor {

    protected final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;

    protected AbstractDispatcherPreSendProcessor(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService) {

        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
    }

    protected Map<String, Long> getConnectionIdMap(
        Long integrationInstanceId, String workflowId, String workflowNodeName) {

        List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflowConnections(
                integrationInstanceId, workflowId, workflowNodeName);

        return MapUtils.toMap(
            integrationInstanceConfigurationWorkflowConnections,
            IntegrationInstanceConfigurationWorkflowConnection::getWorkflowConnectionKey,
            IntegrationInstanceConfigurationWorkflowConnection::getConnectionId);
    }
}

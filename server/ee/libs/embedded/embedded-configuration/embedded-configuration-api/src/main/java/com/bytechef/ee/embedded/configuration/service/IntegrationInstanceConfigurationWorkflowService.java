/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceConfigurationWorkflowService {

    IntegrationInstanceConfigurationWorkflow create(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);

    List<IntegrationInstanceConfigurationWorkflow> create(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows);

    void delete(long id);

    Optional<IntegrationInstanceConfigurationWorkflowConnection>
        fetchIntegrationInstanceConfigurationWorkflowConnection(
            long integrationInstanceConfigurationId, String workflowId, String workflowNodeName,
            String workflowConnectionKey);

    IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(long id);

    IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId);

    IntegrationInstanceConfigurationWorkflowConnection getIntegrationInstanceConfigurationWorkflowConnection(
        long integrationInstanceConfigurationId, String workflowId, String operationName, String key);

    List<IntegrationInstanceConfigurationWorkflowConnection> getIntegrationInstanceConfigurationWorkflowConnections(
        Long integrationInstanceConfigurationId, String workflowId, String operationName);

    List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId);

    List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        List<Long> integrationInstanceConfigurationIds);

    boolean isConnectionUsed(long connectionId);

    boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId);

    IntegrationInstanceConfigurationWorkflow update(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);

    List<IntegrationInstanceConfigurationWorkflow> update(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows);

    void updateEnabled(Long id, boolean enable);
}

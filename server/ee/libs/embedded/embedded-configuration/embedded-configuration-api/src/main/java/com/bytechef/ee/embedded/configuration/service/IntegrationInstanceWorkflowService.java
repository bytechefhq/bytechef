/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationInstanceWorkflowService {

    IntegrationInstanceWorkflow createIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId);

    void delete(Long id);

    void deleteByIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId);

    Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowId);

    IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId);

    List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId);

    List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(List<Long> integrationInstanceIds);

    void update(IntegrationInstanceWorkflow integrationInstanceWorkflow);

    void updateEnabled(Long id, boolean enabled);

}

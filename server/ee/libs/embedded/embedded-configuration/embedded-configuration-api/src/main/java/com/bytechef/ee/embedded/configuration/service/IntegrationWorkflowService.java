/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.platform.constant.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationWorkflowService {

    IntegrationWorkflow addWorkflow(long integrationId, int integrationVersion, String workflowId);

    IntegrationWorkflow addWorkflow(
        long integrationId, int integrationVersion, String workflowId, String workflowReferenceCode);

    void delete(List<Long> ids);

    void delete(long integrationId, int integrationVersion, String workflowId);

    IntegrationWorkflow getIntegrationWorkflow(long id);

    List<Long> getIntegrationWorkflowIds(long integrationId, int integrationVersion);

    List<IntegrationWorkflow> getIntegrationWorkflows();

    List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId);

    List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId, int integrationVersion);

    String getLatestWorkflowId(String workflowReferenceCode);

    String getLatestWorkflowId(String workflowReferenceCode, Environment environment);

    String getWorkflowId(long integrationInstanceId, String workflowReferenceCode);

    List<String> getWorkflowIds(long integrationId, int integrationVersion);

    IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId);

    IntegrationWorkflow update(IntegrationWorkflow integrationWorkflow);
}

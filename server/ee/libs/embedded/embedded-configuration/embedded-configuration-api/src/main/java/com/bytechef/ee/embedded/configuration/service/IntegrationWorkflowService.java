/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationWorkflowService {

    IntegrationWorkflow addWorkflow(long integrationId, int integrationVersion, String workflowId);

    void delete(List<Long> ids);

    void delete(long integrationId, int integrationVersion, String workflowId);

    /**
     * Non-throwing sibling of {@link #getLastWorkflowId(String, Environment)}, used only for branch detection: an empty
     * result means the uuid is not an integration workflow, allowing the caller to fall through to another resolution
     * path. The throwing method stays byte-for-byte unchanged for its existing callers.
     */
    Optional<String> fetchLastWorkflowId(String workflowUuid, Environment environment);

    /**
     * Non-throwing sibling of {@link #getWorkflowIntegrationWorkflow(String)}: an empty result means the id is not an
     * integration workflow, letting a caller fall through to another resolution path without a rollback-poisoning
     * exception. The throwing method is unchanged for its existing callers.
     */
    Optional<IntegrationWorkflow> fetchWorkflowIntegrationWorkflow(String workflowId);

    IntegrationWorkflow getIntegrationWorkflow(long id);

    List<Long> getIntegrationWorkflowIds(long integrationId, int integrationVersion);

    List<IntegrationWorkflow> getIntegrationWorkflows();

    List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId);

    List<IntegrationWorkflow> getIntegrationWorkflows(List<Long> integrationIds);

    List<IntegrationWorkflow> getIntegrationWorkflows(long integrationId, int integrationVersion);

    String getLastWorkflowId(String workflowUuid);

    String getLastWorkflowId(String workflowUuid, Environment environment);

    String getWorkflowId(long integrationInstanceId, String workflowUuid);

    List<String> getWorkflowIds(long integrationId, int integrationVersion);

    IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId);

    void publishWorkflow(
        long integrationId, int oldIntegrationVersion, String oldWorkflowId, IntegrationWorkflow integrationWorkflow);

    IntegrationWorkflow update(IntegrationWorkflow integrationWorkflow);

    IntegrationWorkflow updatePermissionExpression(long id, String permissionExpression);
}

/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.service;

import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.embedded.configuration.service.IntegrationWorkflowService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationWorkflowServiceClient implements IntegrationWorkflowService {

    @Override
    public IntegrationWorkflow addWorkflow(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteIntegrationWorkflows(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getIntegrationWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationWorkflow> getIntegrationWorkflows(long projectId, int lastVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getWorkflowIds(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow getIntegrationWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeWorkflow(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow update(IntegrationWorkflow projectWorkflow) {
        throw new UnsupportedOperationException();
    }
}

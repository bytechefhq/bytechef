/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.constant.Environment;
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
    public IntegrationWorkflow addWorkflow(
        long integrationId, int integrationVersion, String workflowId, String workflowReferenceCode) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow getIntegrationWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getWorkflowId(
        long integrationInstanceId, String workflowReferenceCode) {

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
    public String getLatestWorkflowId(String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLatestWorkflowId(String workflowReferenceCode, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow getWorkflowIntegrationWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationWorkflow update(IntegrationWorkflow projectWorkflow) {
        throw new UnsupportedOperationException();
    }
}

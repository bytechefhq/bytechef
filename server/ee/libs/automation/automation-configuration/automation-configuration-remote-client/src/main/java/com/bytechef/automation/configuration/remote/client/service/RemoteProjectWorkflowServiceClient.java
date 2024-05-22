/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectWorkflowServiceClient implements ProjectWorkflowService {

    @Override
    public ProjectWorkflow addWorkflow(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow addWorkflow(
        long projectId, int projectVersion, String workflowId, String workflowReferenceCode) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectWorkflows(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getProjectWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectWorkflowId(long projectInstanceId, String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getProjectWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectWorkflow> getProjectWorkflows(long projectId, int lastVersion) {
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
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeWorkflow(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow update(ProjectWorkflow projectWorkflow) {
        throw new UnsupportedOperationException();
    }
}

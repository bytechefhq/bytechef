/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import java.util.List;
import java.util.Optional;
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
    public void delete(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<String> fetchLatestProjectWorkflowId(Long projectId, String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectWorkflow>
        fetchProjectWorkflow(long projectId, int projectVersion, String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLatestWorkflowId(String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getLatestProjectWorkflow(Long projectId, String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getProjectWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectDeploymentWorkflowId(long projectDeploymentId, String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getProjectProjectWorkflowIds(long projectId, int projectVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectDeploymentWorkflowReferenceCode(long projectDeploymentId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProjectWorkflowIds(long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProjectWorkflowIds(long projectId, int projectVersion) {
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
    public List<ProjectWorkflow> getProjectWorkflows(Long projectId, String workflowReferenceCode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow getWorkflowProjectWorkflow(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long projectId, int projectVersion, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectWorkflow update(ProjectWorkflow projectWorkflow) {
        throw new UnsupportedOperationException();
    }
}

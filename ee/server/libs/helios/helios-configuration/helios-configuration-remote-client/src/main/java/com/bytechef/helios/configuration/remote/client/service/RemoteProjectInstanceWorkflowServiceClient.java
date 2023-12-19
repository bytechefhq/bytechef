/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.helios.configuration.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.service.ProjectInstanceWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteProjectInstanceWorkflowServiceClient implements ProjectInstanceWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_INSTANCE_WORKFLOW_SERVICE = "/remote/project-instance-workflow-service";
    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectInstanceWorkflowServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<ProjectInstanceWorkflow> create(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectInstanceWorkflowConnection> fetchProjectInstanceWorkflowConnection(
        long projectInstanceId, String workflowId, String workflowConnectionOperationName,
        String workflowConnectionKey) {

        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(
                        PROJECT_INSTANCE_WORKFLOW_SERVICE +
                            "/fetch-project-instance-workflow-connection/{workflowId}/" +
                            "{workflowConnectionOperationName}/{workflowConnectionKey}")
                    .build(workflowId, workflowConnectionOperationName, workflowConnectionKey),
                ProjectInstanceWorkflowConnection.class));
    }

    @Override
    public boolean isProjectInstanceWorkflowEnabled(long projectInstanceId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstanceWorkflow getProjectInstanceWorkflow(long projectInstanceId, String workflowId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE +
                        "/get-project-instance-workflow/{projectInstanceId}/{workflowId}")
                .build(projectInstanceId, workflowId),
            ProjectInstanceWorkflow.class);
    }

    @Override
    public ProjectInstanceWorkflowConnection getProjectInstanceWorkflowConnection(
        long projectInstanceOd, String workflowId, String workflowConnectionOperationName,
        String workflowConnectionKey) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE +
                        "/get-project-instance-workflow-connection/{workflowConnectionOperationName}" +
                        "/{workflowConnectionKey}")
                .build(workflowConnectionOperationName, workflowConnectionKey),
            ProjectInstanceWorkflowConnection.class);
    }

    @Override
    public long getProjectInstanceWorkflowConnectionId(
        long projectInstanceId, String workflowId, String workflowConnectionOperationName,
        String workflowConnectionKey) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE + "/get-project-instance-workflow-connection-id" +
                        "/{workflowConnectionOperationName}/{workflowConnectionKey}")
                .build(workflowConnectionOperationName, workflowConnectionKey),
            Long.class);
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(long projectInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstanceWorkflow> getProjectInstanceWorkflows(List<Long> projectInstanceIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectInstanceWorkflow update(ProjectInstanceWorkflow projectInstanceWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectInstanceWorkflow> update(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enable) {
        throw new UnsupportedOperationException();
    }
}

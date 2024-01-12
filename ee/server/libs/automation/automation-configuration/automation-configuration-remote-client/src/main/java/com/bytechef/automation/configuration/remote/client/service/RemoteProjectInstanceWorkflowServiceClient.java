/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectInstanceWorkflowService;
import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
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
        long projectInstanceId, String workflowId, String operationName,
        String key) {

        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(
                        PROJECT_INSTANCE_WORKFLOW_SERVICE +
                            "/fetch-project-instance-workflow-connection/{projectInstanceId}/{workflowId}/" +
                            "{workflowConnectionOperationName}/{workflowConnectionKey}")
                    .build(projectInstanceId, workflowId, operationName, key),
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
        long projectInstanceOd, String workflowId, String operationName, String key) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE +
                        "/get-project-instance-workflow-connection/{projectInstanceId}/{workflowId}/" +
                        "{workflowConnectionOperationName}/{workflowConnectionKey}")
                .build(projectInstanceOd, workflowId, operationName, key),
            ProjectInstanceWorkflowConnection.class);
    }

    @Override
    public List<ProjectInstanceWorkflowConnection> getProjectInstanceWorkflowConnections(
        long projectInstanceOd, String workflowId, String operationName) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_INSTANCE_WORKFLOW_SERVICE +
                        "/get-project-instance-workflow-connection/{projectInstanceId}/{workflowId}/" +
                        "{workflowConnectionOperationName}")
                .build(projectInstanceOd, workflowId, operationName),
            new ParameterizedTypeReference<>() {});
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

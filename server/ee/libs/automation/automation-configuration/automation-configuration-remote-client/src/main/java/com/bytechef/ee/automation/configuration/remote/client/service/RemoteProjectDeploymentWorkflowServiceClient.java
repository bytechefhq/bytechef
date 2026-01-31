/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class RemoteProjectDeploymentWorkflowServiceClient implements ProjectDeploymentWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_DEPLOYMENT_WORKFLOW_SERVICE = "/remote/project/deployment-workflow-service";
    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteProjectDeploymentWorkflowServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<ProjectDeploymentWorkflow> create(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeploymentWorkflow create(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProjectDeploymentWorkflowConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProjectDeploymentWorkflow> fetchProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProjectDeploymentWorkflowEnabled(long projectDeploymentId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentId, String workflowId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_DEPLOYMENT_WORKFLOW_SERVICE +
                        "/get-project/deployment-workflow/{projectDeploymentId}/{workflowId}")
                .build(projectDeploymentId, workflowId),
            ProjectDeploymentWorkflow.class);
    }

    @Override
    public Optional<ProjectDeploymentWorkflowConnection> fetchProjectDeploymentWorkflowConnection(
        long projectDeploymentOd, String workflowId, String workflowNodeName, String workflowConnectionKey) {

        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(
                        PROJECT_DEPLOYMENT_WORKFLOW_SERVICE +
                            "/fetch-project/deployment-workflow-connection/{projectDeploymentId}/{workflowId}/" +
                            "{workflowNodeName}/{workflowConnectionKey}")
                    .build(projectDeploymentOd, workflowId, workflowNodeName, workflowConnectionKey),
                ProjectDeploymentWorkflowConnection.class));
    }

    @Override
    public ProjectDeploymentWorkflow getProjectDeploymentWorkflow(long projectDeploymentWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflowConnection> getProjectDeploymentWorkflowConnections(
        long projectDeploymentId, String workflowId, String workflowNodeName) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_DEPLOYMENT_WORKFLOW_SERVICE +
                        "/get-project/deployment-workflow-connection/{projectDeploymentId}/{workflowId}/" +
                        "{workflowNodeName}")
                .build(projectDeploymentId, workflowId, workflowNodeName),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(long projectDeploymentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflow> getProjectDeploymentWorkflows(List<Long> projectDeploymentIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    PROJECT_DEPLOYMENT_WORKFLOW_SERVICE +
                        "/is-connection-used/{connectionId}")
                .build(connectionId),
            Boolean.class);
    }

    @Override
    public ProjectDeploymentWorkflow update(ProjectDeploymentWorkflow projectDeploymentWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProjectDeploymentWorkflow> update(List<ProjectDeploymentWorkflow> projectDeploymentWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enable) {
        throw new UnsupportedOperationException();
    }
}

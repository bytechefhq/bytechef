/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflowConnection;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
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
public class RemoteIntegrationInstanceWorkflowServiceClient implements IntegrationInstanceWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String INTEGRATION_INSTANCE_WORKFLOW_SERVICE = "/remote/integration-instance-workflow-service";
    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceWorkflowServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public List<IntegrationInstanceWorkflow> create(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstanceWorkflowConnection> fetchIntegrationInstanceWorkflowConnection(
        long integrationInstanceId, String workflowId, String operationName,
        String key) {

        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(
                        INTEGRATION_INSTANCE_WORKFLOW_SERVICE +
                            "/fetch-integration-instance-workflow-connection/{integrationInstanceId}/{workflowId}/" +
                            "{workflowConnectionOperationName}/{workflowConnectionKey}")
                    .build(integrationInstanceId, workflowId, operationName, key),
                IntegrationInstanceWorkflowConnection.class));
    }

    @Override
    public boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(long integrationInstanceId, String workflowId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    INTEGRATION_INSTANCE_WORKFLOW_SERVICE +
                        "/get-integration-instance-workflow/{integrationInstanceId}/{workflowId}")
                .build(integrationInstanceId, workflowId),
            IntegrationInstanceWorkflow.class);
    }

    @Override
    public IntegrationInstanceWorkflowConnection getIntegrationInstanceWorkflowConnection(
        long integrationInstanceId, String workflowId, String operationName,
        String key) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    INTEGRATION_INSTANCE_WORKFLOW_SERVICE +
                        "/get-integration-instance-workflow-connection/{integrationInstanceId}/{workflowId}" +
                        "/{workflowConnectionOperationName}/{workflowConnectionKey}")
                .build(integrationInstanceId, operationName, key),
            IntegrationInstanceWorkflowConnection.class);
    }

    @Override
    public List<IntegrationInstanceWorkflowConnection> getIntegrationInstanceWorkflowConnections(
        Long integrationInstanceId, String workflowId, String operationName) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    INTEGRATION_INSTANCE_WORKFLOW_SERVICE +
                        "/get-integration-instance-workflow-connection/{integrationInstanceId}/{workflowId}" +
                        "/{workflowConnectionOperationName}")
                .build(integrationInstanceId, operationName),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(List<Long> integrationInstanceIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceWorkflow update(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceWorkflow> update(List<IntegrationInstanceWorkflow> integrationInstanceWorkflows) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enable) {
        throw new UnsupportedOperationException();
    }
}

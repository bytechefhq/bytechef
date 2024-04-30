/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
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
public class RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient
    implements IntegrationInstanceConfigurationWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_SERVICE =
        "/remote/integration-instance-configuration-workflow-service";
    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceConfigurationConfigurationWorkflowServiceClient(
        LoadBalancedRestClient loadBalancedRestClient) {

        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow create(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> create(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstanceConfigurationWorkflowConnection>
        fetchIntegrationInstanceConfigurationWorkflowConnection(
            long integrationInstanceConfigurationId, String workflowId, String workflowNodeName,
            String workflowConnectionKey) {

        return Optional.ofNullable(
            loadBalancedRestClient.get(
                uriBuilder -> uriBuilder
                    .host(CONFIGURATION_APP)
                    .path(
                        INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_SERVICE +
                            "/fetch-integration-instance-configuration-workflow-connection/{projectInstanceId}/" +
                            "{workflowId}/{workflowNodeName}/{workflowConnectionKey}")
                    .build(integrationInstanceConfigurationId, workflowId, workflowNodeName, workflowConnectionKey),
                IntegrationInstanceConfigurationWorkflowConnection.class));
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow getIntegrationInstanceConfigurationWorkflow(
        long integrationInstanceConfigurationId, String workflowId) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_SERVICE +
                        "/get-integration-instance-configuration-workflow/{integrationInstanceConfigurationId}" +
                        "/{workflowId}")
                .build(integrationInstanceConfigurationId, workflowId),
            IntegrationInstanceConfigurationWorkflow.class);
    }

    @Override
    public IntegrationInstanceConfigurationWorkflowConnection getIntegrationInstanceConfigurationWorkflowConnection(
        long integrationInstanceConfigurationId, String workflowId, String operationName,
        String key) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_SERVICE +
                        "/get-integration-instance-configuration-workflow-connection/" +
                        "{integrationInstanceConfigurationId}/{workflowId}/{operationName}/{key}")
                .build(integrationInstanceConfigurationId, workflowId, operationName, key),
            IntegrationInstanceConfigurationWorkflowConnection.class);
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflowConnection>
        getIntegrationInstanceConfigurationWorkflowConnections(
            Long integrationInstanceConfigurationId, String workflowId, String operationName) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(
                    INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_SERVICE +
                        "/get-integration-instance-configuration-workflow-connection/" +
                        "{integrationInstanceConfigurationId}/{workflowId}/{operationName}")
                .build(integrationInstanceConfigurationId, workflowId, operationName),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> getIntegrationInstanceConfigurationWorkflows(
        List<Long> integrationInstanceConfigurationIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_SERVICE + "/is-connection-used/{connectionId}")
                .build(connectionId),
            Boolean.class);
    }

    @Override
    public boolean isIntegrationInstanceWorkflowEnabled(long integrationInstanceId, String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfigurationWorkflow update(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfigurationWorkflow> update(
        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enable) {
        throw new UnsupportedOperationException();
    }
}

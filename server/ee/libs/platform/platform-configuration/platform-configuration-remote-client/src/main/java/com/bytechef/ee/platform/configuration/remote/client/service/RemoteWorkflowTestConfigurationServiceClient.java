/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteWorkflowTestConfigurationServiceClient implements WorkflowTestConfigurationService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String WORKFLOW_SERVICE = "/remote/workflow-test-configuration-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteWorkflowTestConfigurationServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public void delete(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String workflowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkflowTestConfigurationConnection(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<WorkflowTestConfiguration> fetchWorkflowTestConfiguration(String workflowId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Long> fetchWorkflowTestConfigurationConnectionId(
        String workflowId, String workflowNodeName, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        String workflowId, String workflowNodeName, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<WorkflowTestConfiguration> getWorkflowTestConfigurations(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(WORKFLOW_SERVICE + "/is-connection-used/{connectionId}")
                .build(connectionId),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public WorkflowTestConfiguration saveWorkflowTestConfiguration(
        WorkflowTestConfiguration workflowTestConfiguration) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ?> getWorkflowTestConfigurationInputs(String workflowId, long environmentId) {
        return null;
    }

    @Override
    public void deleteWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void saveWorkflowTestConfigurationConnection(
        String workflowId, String workflowNodeName, String key, long connectionId, boolean workflowNodeTrigger,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void saveWorkflowTestConfigurationInputs(String workflowId, String key, String value, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateWorkflowId(String oldWorkflowId, String newWorkflowId) {
        throw new UnsupportedOperationException();
    }
}

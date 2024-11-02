/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.service.IntegrationInstanceWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationInstanceWorkflowServiceClient implements IntegrationInstanceWorkflowService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_SERVICE = "/remote/integration-instance-workflow-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceWorkflowServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public IntegrationInstanceWorkflow createIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IntegrationInstanceWorkflow> fetchIntegrationInstanceWorkflow(
        long integrationInstanceId, @NonNull String workflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceWorkflow getIntegrationInstanceWorkflow(
        long integrationInstanceId, @NonNull String workflowId) {

        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-integration/instance-workflow/{integrationInstanceId}/{workflowId}")
                .build(integrationInstanceId, workflowId),
            IntegrationInstanceWorkflow.class);
    }

    @Override
    public List<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(Long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}

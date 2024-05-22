/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.embedded.configuration.service.IntegrationService;
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
public class RemoteIntegrationServiceClient implements IntegrationService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_SERVICE = "/remote/integration-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public int addVersion(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countIntegrations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration create(Integration integration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Integration> fetchWorkflowIntegration(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Integration> fetchIntegration(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration getIntegrationInstanceIntegration(long integrationInstanceId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-integration-instance-integration/{integrationInstanceId}")
                .build(integrationInstanceId),
            Integration.class);
    }

    @Override
    public Integration getIntegration(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-integration/{id}")
                .build(id),
            Integration.class);
    }

    @Override
    public List<Integration> getIntegrations() {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-integrations")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Integration> getIntegrations(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationVersion> getIntegrationVersions(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Integration> getIntegrations(Long categoryId, List<Long> ids, Long tagId, Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration getWorkflowIntegration(String workflowId) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_SERVICE + "/get-workflow-integration/{workflowId}")
                .build(workflowId),
            Integration.class);
    }

    @Override
    public Integration publishIntegration(long id, String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integration update(Integration integration) {
        throw new UnsupportedOperationException();
    }
}

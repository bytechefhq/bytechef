/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationInstanceServiceClient implements IntegrationInstanceService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_INSTANCE_SERVICE = "/remote/integration-instance-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public IntegrationInstance create(IntegrationInstance integrationInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIntegrationInstanceEnabled(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance getIntegrationInstance(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_INSTANCE_SERVICE + "/get-integration-instance/{id}")
                .build(id),
            IntegrationInstance.class);
    }

    @Override
    public List<Long> getIntegrationIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstance> getIntegrationInstances(Long integrationId, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstance update(IntegrationInstance integrationInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}

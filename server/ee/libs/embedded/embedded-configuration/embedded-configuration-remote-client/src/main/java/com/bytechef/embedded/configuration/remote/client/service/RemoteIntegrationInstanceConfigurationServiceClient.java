/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.embedded.configuration.remote.client.service;

import com.bytechef.commons.rest.client.LoadBalancedRestClient;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationInstanceConfigurationServiceClient implements IntegrationInstanceConfigurationService {

    private static final String CONFIGURATION_APP = "configuration-app";
    private static final String PROJECT_INSTANCE_SERVICE = "/remote/integration-instance-configuration-service";

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteIntegrationInstanceConfigurationServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public IntegrationInstanceConfiguration create(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIntegrationInstanceConfigurationEnabled(long integrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host(CONFIGURATION_APP)
                .path(PROJECT_INSTANCE_SERVICE + "/get-integration-instance-configuration/{id}")
                .build(id),
            IntegrationInstanceConfiguration.class);
    }

    @Override
    public List<Long> getIntegrationIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(long integrationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration> getIntegrationInstanceConfigurations(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstanceConfiguration>
        getIntegrationInstanceConfigurations(Environment environment, Long integrationId, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfiguration update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceConfiguration update(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}

/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteIntegrationInstanceServiceClient implements IntegrationInstanceService {

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(long connectedUserId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<IntegrationInstance> getConnectedUserIntegrationInstances(List<Long> connectedUserIds) {
        return null;
    }

    @Override
    public IntegrationInstance getIntegrationInstance(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegrationInstanceWorkflow updateWorkflowEnabled(
        long id, long integrationInstanceConfigurationWorkflowId, boolean enable) {

        throw new UnsupportedOperationException();
    }
}

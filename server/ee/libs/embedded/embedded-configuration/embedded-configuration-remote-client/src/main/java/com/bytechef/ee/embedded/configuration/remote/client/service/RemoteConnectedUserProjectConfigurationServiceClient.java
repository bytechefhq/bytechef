/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteConnectedUserProjectConfigurationServiceClient implements ConnectedUserProjectConfigurationService {

    @Override
    public ConnectedUserProjectConfiguration assign(long connectedUserId, long projectId, long configurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, Integer> countByConfigurationIds(Collection<Long> configurationIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByConfigurationId(long configurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ConnectedUserProjectConfiguration> fetchAssignment(long connectedUserId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectConfiguration> getAssignments(long configurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProjectConfiguration> getConnectedUserAssignments(long connectedUserId) {
        throw new UnsupportedOperationException();
    }
}

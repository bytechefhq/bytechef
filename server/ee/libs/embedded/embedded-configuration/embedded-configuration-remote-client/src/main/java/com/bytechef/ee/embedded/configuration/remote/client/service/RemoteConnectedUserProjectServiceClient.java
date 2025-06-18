/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectedUserProjectServiceClient implements ConnectedUserProjectService {

    @Override
    public boolean containsProjectDeployment(long projectDeploymentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject create(ConnectedUserProject connectedUserProject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject create(long connectedUserId, long projectId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ConnectedUserProject> fetchConnectUserProject(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject getConnectedUserProject(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject getConnectedUserConnectedUserProject(Long connectedUserid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUserProject getConnectUserProject(String externalUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ConnectedUserProject> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        throw new UnsupportedOperationException();
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.platform.constant.Environment;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserProjectServiceImpl implements ConnectedUserProjectService {

    private final ConnectUserProjectRepository connectUserProjectRepository;

    public ConnectedUserProjectServiceImpl(ConnectUserProjectRepository connectUserProjectRepository) {
        this.connectUserProjectRepository = connectUserProjectRepository;
    }

    @Override
    public boolean containsProjectDeployment(long projectDeploymentId) {
        return connectUserProjectRepository.existsByProjectDeploymentId(projectDeploymentId);
    }

    @Override
    public ConnectedUserProject create(ConnectedUserProject connectedUserProject) {
        return connectUserProjectRepository.save(connectedUserProject);
    }

    @Override
    public ConnectedUserProject create(long connectedUserId, long projectId) {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setConnectedUserId(connectedUserId);
        connectedUserProject.setProjectId(projectId);

        return connectUserProjectRepository.save(connectedUserProject);
    }

    @Override
    public void delete(Long id) {
        connectUserProjectRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserProject getConnectedUserProject(Long id) {
        return OptionalUtils.get(connectUserProjectRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserProject getConnectedUserConnectedUserProject(Long connectedUserid) {
        return OptionalUtils.get(connectUserProjectRepository.findByConnectedUserId(connectedUserid));
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserProject getConnectUserProject(String externalUserId, Environment environment) {
        return OptionalUtils.get(fetchConnectUserProject(externalUserId, environment));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConnectedUserProject> fetchConnectUserProject(String externalUserId, Environment environment) {
        return connectUserProjectRepository.findFirstByEnvironmentAndExternalUserId(
            externalUserId, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserProject> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        // TODO Add query
        return CollectionUtils.toList(connectUserProjectRepository.findAll());
    }
}

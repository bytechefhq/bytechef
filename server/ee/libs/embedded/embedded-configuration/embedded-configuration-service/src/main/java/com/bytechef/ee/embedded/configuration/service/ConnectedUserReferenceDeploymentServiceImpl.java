/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserReferenceDeploymentRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@ConditionalOnEEVersion
public class ConnectedUserReferenceDeploymentServiceImpl implements ConnectedUserReferenceDeploymentService {

    private final ConnectedUserReferenceDeploymentRepository connectedUserReferenceDeploymentRepository;

    public ConnectedUserReferenceDeploymentServiceImpl(
        ConnectedUserReferenceDeploymentRepository connectedUserReferenceDeploymentRepository) {

        this.connectedUserReferenceDeploymentRepository = connectedUserReferenceDeploymentRepository;
    }

    @Override
    public ConnectedUserReferenceDeployment create(long connectedUserId, long projectId, long projectDeploymentId) {
        return connectedUserReferenceDeploymentRepository.save(
            new ConnectedUserReferenceDeployment(connectedUserId, projectId, projectDeploymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConnectedUserReferenceDeployment> fetchConnectedUserReferenceDeployment(
        long connectedUserId, long projectId) {

        return connectedUserReferenceDeploymentRepository.findByConnectedUserIdAndProjectId(connectedUserId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReferenceDeployment(long projectDeploymentId) {
        return connectedUserReferenceDeploymentRepository.existsByProjectDeploymentId(projectDeploymentId);
    }
}

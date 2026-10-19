/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import java.util.List;
import java.util.Optional;

/**
 * Identifies a connected user's reference deployment of an automation workflow project by relation rather than by the
 * deployment's name.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserReferenceDeploymentService {

    ConnectedUserReferenceDeployment create(long connectedUserId, long projectId, long projectDeploymentId);

    Optional<ConnectedUserReferenceDeployment> fetchConnectedUserReferenceDeployment(
        long connectedUserId, long projectId);

    List<ConnectedUserReferenceDeployment> getConnectedUserReferenceDeployments(long connectedUserId);

    boolean isReferenceDeployment(long projectDeploymentId);
}

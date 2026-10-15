/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserReferenceDeployment;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Repository
public interface ConnectedUserReferenceDeploymentRepository
    extends ListCrudRepository<ConnectedUserReferenceDeployment, Long> {

    boolean existsByProjectDeploymentId(long projectDeploymentId);

    Optional<ConnectedUserReferenceDeployment> findByConnectedUserIdAndProjectId(long connectedUserId, long projectId);
}

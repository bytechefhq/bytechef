/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserProjectConfigurationService {

    /**
     * Assigns the connected user to the configuration, inserting a row or moving the existing row of the user and
     * project to the configuration.
     *
     * @param connectedUserId the connected user id
     * @param projectId       the project id
     * @param configurationId the automation workflow project configuration id
     * @return the saved assignment
     */
    ConnectedUserProjectConfiguration assign(long connectedUserId, long projectId, long configurationId);

    Map<Long, Integer> countByConfigurationIds(Collection<Long> configurationIds);

    void deleteByConfigurationId(long configurationId);

    Optional<ConnectedUserProjectConfiguration> fetchAssignment(long connectedUserId, long projectId);

    List<ConnectedUserProjectConfiguration> getAssignments(long configurationId);

    /**
     * Returns the connected user's assignments.
     *
     * @param connectedUserId the connected user id
     * @return the connected user's assignments, one per automation workflow project
     */
    List<ConnectedUserProjectConfiguration> getConnectedUserAssignments(long connectedUserId);
}

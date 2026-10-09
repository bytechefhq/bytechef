/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserConnectionMembership {

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectionService connectionService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionMembership(
        ConnectedUserConnectionService connectedUserConnectionService, ConnectionService connectionService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectionService = connectionService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Transactional(readOnly = true)
    public Set<Long> getConnectionIds(long connectedUserId, Environment environment) {
        Set<Long> connectionIds = getUnfilteredOwnedConnectionIds(connectedUserId, environment);

        connectionIds.addAll(connectedUserConnectionService.getSharedConnectionIds());

        return filterByEnvironment(connectionIds, environment);
    }

    @Transactional(readOnly = true)
    public Set<Long> getOwnedConnectionIds(long connectedUserId, Environment environment) {
        return filterByEnvironment(getUnfilteredOwnedConnectionIds(connectedUserId, environment), environment);
    }

    private Set<Long> filterByEnvironment(Set<Long> connectionIds, Environment environment) {
        Set<Long> filteredConnectionIds = new LinkedHashSet<>();

        if (connectionIds.isEmpty()) {
            return filteredConnectionIds;
        }

        Set<Long> sameEnvironmentConnectionIds = new HashSet<>();

        for (Connection connection : connectionService.getConnections(new ArrayList<>(connectionIds))) {
            if (connection.getType() == PlatformType.EMBEDDED &&
                connection.getEnvironmentId() == environment.ordinal()) {

                sameEnvironmentConnectionIds.add(connection.getId());
            }
        }

        for (Long connectionId : connectionIds) {
            if (sameEnvironmentConnectionIds.contains(connectionId)) {
                filteredConnectionIds.add(connectionId);
            }
        }

        return filteredConnectionIds;
    }

    private Set<Long> getUnfilteredOwnedConnectionIds(long connectedUserId, Environment environment) {
        Set<Long> connectionIds = new LinkedHashSet<>();

        for (IntegrationInstance integrationInstance : integrationInstanceService.getConnectedUserIntegrationInstances(
            connectedUserId, environment)) {

            connectionIds.add(integrationInstance.getConnectionId());
        }

        connectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUserId));

        return connectionIds;
    }
}

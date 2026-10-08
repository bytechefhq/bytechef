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
import java.util.LinkedHashSet;
import java.util.List;
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
        List<IntegrationInstance> integrationInstances =
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, environment);

        Set<Long> connectionIds = getOwnedConnectionIds(connectedUserId, integrationInstances);

        Set<Long> sharedConnectionIds = connectedUserConnectionService.getSharedConnectionIds();

        if (!sharedConnectionIds.isEmpty()) {
            for (Connection connection : connectionService.getConnections(new ArrayList<>(sharedConnectionIds))) {
                if (connection.getType() == PlatformType.EMBEDDED &&
                    connection.getEnvironmentId() == environment.ordinal()) {

                    connectionIds.add(connection.getId());
                }
            }
        }

        return connectionIds;
    }

    @Transactional(readOnly = true)
    public Set<Long> getOwnedConnectionIds(long connectedUserId, Environment environment) {
        return getOwnedConnectionIds(
            connectedUserId,
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, environment));
    }

    private Set<Long> getOwnedConnectionIds(long connectedUserId, List<IntegrationInstance> integrationInstances) {
        Set<Long> connectionIds = new LinkedHashSet<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            connectionIds.add(integrationInstance.getConnectionId());
        }

        connectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUserId));

        return connectionIds;
    }
}

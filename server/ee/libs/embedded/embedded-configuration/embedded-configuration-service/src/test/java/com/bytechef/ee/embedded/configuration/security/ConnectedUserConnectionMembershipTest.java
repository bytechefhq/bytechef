/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserConnectionMembershipTest {

    private static final long CONNECTED_USER_ID = 7L;
    private static final long INSTANCE_CONNECTION_ID = 10L;
    private static final long OTHER_ENVIRONMENT_SHARED_CONNECTION_ID = 13L;
    private static final long OWNED_CONNECTION_ID = 11L;
    private static final long SHARED_CONNECTION_ID = 12L;

    private final ConnectedUserConnectionService connectedUserConnectionService =
        mock(ConnectedUserConnectionService.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);

    private ConnectedUserConnectionMembership connectedUserConnectionMembership;

    @BeforeEach
    void beforeEach() {
        connectedUserConnectionMembership = new ConnectedUserConnectionMembership(
            connectedUserConnectionService, connectionService, integrationInstanceService);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectionId()).thenReturn(INSTANCE_CONNECTION_ID);
        when(integrationInstanceService.getConnectedUserIntegrationInstances(
            CONNECTED_USER_ID, Environment.PRODUCTION)).thenReturn(List.of(integrationInstance));
        when(connectedUserConnectionService.getConnectionIds(CONNECTED_USER_ID))
            .thenReturn(List.of(OWNED_CONNECTION_ID));
        when(connectedUserConnectionService.getSharedConnectionIds())
            .thenReturn(Set.of(SHARED_CONNECTION_ID, OTHER_ENVIRONMENT_SHARED_CONNECTION_ID));
        when(connectionService.getConnections(anyList())).thenReturn(
            List.of(
                getConnection(INSTANCE_CONNECTION_ID, Environment.PRODUCTION),
                getConnection(OWNED_CONNECTION_ID, Environment.PRODUCTION),
                getConnection(SHARED_CONNECTION_ID, Environment.PRODUCTION),
                getConnection(OTHER_ENVIRONMENT_SHARED_CONNECTION_ID, Environment.DEVELOPMENT)));
    }

    @Test
    void testGetConnectionIdsIncludesOwnedAndSharedConnectionsOfTheSameEnvironment() {
        assertThat(connectedUserConnectionMembership.getConnectionIds(CONNECTED_USER_ID, Environment.PRODUCTION))
            .containsExactlyInAnyOrder(INSTANCE_CONNECTION_ID, OWNED_CONNECTION_ID, SHARED_CONNECTION_ID);
    }

    @Test
    void testGetOwnedConnectionIdsExcludesSharedConnections() {
        assertThat(connectedUserConnectionMembership.getOwnedConnectionIds(CONNECTED_USER_ID, Environment.PRODUCTION))
            .containsExactlyInAnyOrder(INSTANCE_CONNECTION_ID, OWNED_CONNECTION_ID);
    }

    private static Connection getConnection(long id, Environment environment) {
        Connection connection = Connection.builder()
            .componentName("slack")
            .name("Slack " + id)
            .type(PlatformType.EMBEDDED)
            .build();

        connection.setEnvironmentId(environment.ordinal());
        connection.setId(id);

        return connection;
    }
}

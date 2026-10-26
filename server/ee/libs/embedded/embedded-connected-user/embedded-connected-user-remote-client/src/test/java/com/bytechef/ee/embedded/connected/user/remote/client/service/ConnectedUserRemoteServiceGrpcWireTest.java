/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.remote.grpc.ConnectedUserRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ConnectedUserRemoteServiceGrpcWireTest {

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private ManagedChannel managedChannel;
    private RemoteConnectedUserServiceClient remoteConnectedUserServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ConnectedUserRemoteServiceGrpcServer(connectedUserService))
            .build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectedUserServiceClient = new RemoteConnectedUserServiceClient(serviceId -> managedChannel);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testFetchConnectedUser() {
        when(connectedUserService.fetchConnectedUser("ext-user", 2L)).thenReturn(Optional.of(connectedUser()));

        assertThat(remoteConnectedUserServiceClient.fetchConnectedUser("ext-user", 2L)).map(ConnectedUser::getId)
            .contains(9L);
    }

    @Test
    void testFetchConnectedUserReturnsEmptyWhenMissing() {
        when(connectedUserService.fetchConnectedUser("missing", 2L)).thenReturn(Optional.empty());

        assertThat(remoteConnectedUserServiceClient.fetchConnectedUser("missing", 2L)).isEmpty();
    }

    @Test
    void testCreateConnectedUser() {
        when(connectedUserService.createConnectedUser("ext-user", 2L)).thenReturn(connectedUser());

        assertThat(remoteConnectedUserServiceClient.createConnectedUser("ext-user", 2L)
            .getExternalId()).isEqualTo("ext-user");
    }

    @Test
    void testCreateConnectedUserWithReservedIdIsRejected() {
        when(connectedUserService.createConnectedUser("automation", 2L)).thenThrow(
            new IllegalArgumentException(
                "externalId 'automation' is reserved and cannot be used for a connected user"));

        assertThatThrownBy(() -> remoteConnectedUserServiceClient.createConnectedUser("automation", 2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");
    }

    private static ConnectedUser connectedUser() {
        return new ConnectedUser(Map.of(), "ext@example.com", true, "ext-user", 9L, "Ext", 0);
    }
}

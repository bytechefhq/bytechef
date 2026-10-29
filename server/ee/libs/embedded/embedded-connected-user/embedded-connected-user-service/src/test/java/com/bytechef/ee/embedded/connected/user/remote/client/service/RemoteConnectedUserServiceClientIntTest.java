/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.connected.user.config.ConnectedUserIntTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.remote.grpc.ConnectedUserRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ConnectedUserIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=user"
    })
@Import(PostgreSQLContainerConfiguration.class)
class RemoteConnectedUserServiceClientIntTest {

    @Autowired
    private ConnectedUserRepository connectedUserRepository;

    @Autowired
    private ConnectedUserService connectedUserService;

    private ManagedChannel managedChannel;
    private RemoteConnectedUserServiceClient remoteConnectedUserServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(new ConnectedUserRemoteServiceGrpcServer(connectedUserService))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteConnectedUserServiceClient = new RemoteConnectedUserServiceClient(new FixedGrpcChannels(managedChannel));
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        connectedUserRepository.deleteAll();
    }

    @Test
    void testCreateConnectedUserStoresTheConnectedUserInTheRequestedEnvironment() {
        ConnectedUser connectedUser = remoteConnectedUserServiceClient.createConnectedUser(
            "customer-a", Environment.STAGING.ordinal());

        assertThat(connectedUser.getId()).isNotNull();
        assertThat(connectedUser.getExternalId()).isEqualTo("customer-a");
        assertThat(connectedUser.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(connectedUser.isEnabled()).isTrue();

        ConnectedUser storedConnectedUser = connectedUserService.getConnectedUser(connectedUser.getId());

        assertThat(storedConnectedUser.getExternalId()).isEqualTo("customer-a");
        assertThat(storedConnectedUser.getEnvironment()).isEqualTo(Environment.STAGING);
        assertThat(storedConnectedUser.isEnabled()).isTrue();
    }

    @Test
    void testCreateConnectedUserByEnvironmentStoresTheConnectedUserInThatEnvironment() {
        ConnectedUser connectedUser = remoteConnectedUserServiceClient.createConnectedUser(
            "customer-b", Environment.DEVELOPMENT);

        Optional<ConnectedUser> storedConnectedUser = connectedUserService.fetchConnectedUser(
            "customer-b", Environment.DEVELOPMENT);

        assertThat(storedConnectedUser).map(ConnectedUser::getId)
            .contains(connectedUser.getId());
    }

    @Test
    void testCreateConnectedUserWithReservedExternalIdIsRejectedAndNothingIsStored() {
        assertThatThrownBy(
            () -> remoteConnectedUserServiceClient.createConnectedUser("automation", Environment.PRODUCTION.ordinal()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("externalId 'automation' is reserved and cannot be used for a connected user");

        assertThat(connectedUserRepository.count()).isZero();
    }

    @Test
    void testFetchConnectedUserReturnsTheStoredConnectedUserOfThatEnvironment() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser(
            "customer-c", Environment.PRODUCTION, "Acme", "ops@acme.io", Map.of("plan", "gold"));

        connectedUserService.createConnectedUser("customer-c", Environment.STAGING);

        Optional<ConnectedUser> fetchedConnectedUser = remoteConnectedUserServiceClient.fetchConnectedUser(
            "customer-c", Environment.PRODUCTION.ordinal());

        assertThat(fetchedConnectedUser).isPresent();

        ConnectedUser remoteConnectedUser = fetchedConnectedUser.get();

        assertThat(remoteConnectedUser.getId()).isEqualTo(connectedUser.getId());
        assertThat(remoteConnectedUser.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(remoteConnectedUser.getName()).isEqualTo("Acme");
        assertThat(remoteConnectedUser.getEmail()).isEqualTo("ops@acme.io");
        assertThat(remoteConnectedUser.getVersion()).isEqualTo(connectedUser.getVersion());
    }

    @Test
    void testFetchConnectedUserReturnsEmptyWhenTheExternalIdIsUnknownInThatEnvironment() {
        connectedUserService.createConnectedUser("customer-d", Environment.STAGING);

        assertThat(remoteConnectedUserServiceClient.fetchConnectedUser("customer-d", Environment.PRODUCTION))
            .isEmpty();
        assertThat(remoteConnectedUserServiceClient.fetchConnectedUser("unknown", Environment.STAGING.ordinal()))
            .isEmpty();
    }

    @Test
    void testGetConnectedUserReturnsTheStoredConnectedUser() {
        ConnectedUser connectedUser = connectedUserService.createConnectedUser("customer-e", Environment.PRODUCTION);

        ConnectedUser remoteConnectedUser = remoteConnectedUserServiceClient.getConnectedUser(
            "customer-e", Environment.PRODUCTION);

        assertThat(remoteConnectedUser.getId()).isEqualTo(connectedUser.getId());
        assertThat(remoteConnectedUser.getExternalId()).isEqualTo("customer-e");
    }

    @Test
    void testGetConnectedUserWithoutStoredConnectedUserThrows() {
        assertThatThrownBy(() -> remoteConnectedUserServiceClient.getConnectedUser("missing", Environment.PRODUCTION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Connected user not found");
    }
}

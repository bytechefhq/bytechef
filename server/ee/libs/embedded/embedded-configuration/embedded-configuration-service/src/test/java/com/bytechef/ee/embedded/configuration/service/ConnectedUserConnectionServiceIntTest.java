/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserConnection;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserSharedConnection;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserConnectionRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserSharedConnectionRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class ConnectedUserConnectionServiceIntTest {

    @Autowired
    private ConnectedUserConnectionRepository connectedUserConnectionRepository;

    @Autowired
    private ConnectedUserConnectionService connectedUserConnectionService;

    @Autowired
    private ConnectedUserSharedConnectionRepository connectedUserSharedConnectionRepository;

    @AfterEach
    void afterEach() {
        connectedUserConnectionRepository.deleteAll();
        connectedUserSharedConnectionRepository.deleteAll();
    }

    @Test
    void testCreate() {
        connectedUserConnectionService.create(1L, 5L);

        List<ConnectedUserConnection> connectedUserConnections = connectedUserConnectionRepository.findAll();

        assertThat(connectedUserConnections)
            .singleElement()
            .satisfies(connectedUserConnection -> {
                assertThat(connectedUserConnection.getConnectedUserId()).isEqualTo(1L);
                assertThat(connectedUserConnection.getConnectionId()).isEqualTo(5L);
            });
    }

    @Test
    void testGetConnectionIds() {
        connectedUserConnectionService.create(1L, 5L);
        connectedUserConnectionService.create(1L, 6L);
        connectedUserConnectionService.create(2L, 7L);

        assertThat(connectedUserConnectionService.getConnectionIds(1L)).containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    void testDeleteByConnectionIdAlsoRemovesTheSharedFlag() {
        connectedUserConnectionService.create(1L, 5L);
        connectedUserConnectionService.create(1L, 6L);
        connectedUserConnectionService.updateShared(5L, true);
        connectedUserConnectionService.updateShared(6L, true);

        connectedUserConnectionService.deleteByConnectionId(5L);

        assertThat(connectedUserConnectionService.getConnectionIds(1L)).containsExactly(6L);
        assertThat(connectedUserConnectionService.existsByConnectionId(5L)).isFalse();
        assertThat(connectedUserSharedConnectionRepository.existsByConnectionId(5L)).isFalse();
        assertThat(connectedUserConnectionService.getSharedConnectionIds()).containsExactly(6L);
    }

    @Test
    void testGetSharedConnectionIds() {
        connectedUserSharedConnectionRepository.save(new ConnectedUserSharedConnection(7L));
        connectedUserSharedConnectionRepository.save(new ConnectedUserSharedConnection(9L));

        assertThat(connectedUserConnectionService.getSharedConnectionIds()).containsExactlyInAnyOrder(7L, 9L);
    }

    @Test
    void testUpdateSharedMarksAnUnsharedConnectionShared() {
        connectedUserConnectionService.updateShared(5L, true);

        assertThat(connectedUserSharedConnectionRepository.findAll())
            .singleElement()
            .satisfies(connectedUserSharedConnection -> assertThat(connectedUserSharedConnection.getConnectionId())
                .isEqualTo(5L));
    }

    @Test
    void testUpdateSharedLeavesAnAlreadySharedConnectionAlone() {
        ConnectedUserSharedConnection connectedUserSharedConnection =
            connectedUserSharedConnectionRepository.save(new ConnectedUserSharedConnection(5L));

        connectedUserConnectionService.updateShared(5L, true);

        assertThat(connectedUserSharedConnectionRepository.findAll())
            .singleElement()
            .satisfies(sharedConnection -> assertThat(sharedConnection.getId())
                .isEqualTo(connectedUserSharedConnection.getId()));
    }

    @Test
    void testUpdateSharedUnsharesASharedConnection() {
        connectedUserSharedConnectionRepository.save(new ConnectedUserSharedConnection(5L));
        connectedUserSharedConnectionRepository.save(new ConnectedUserSharedConnection(6L));

        connectedUserConnectionService.updateShared(5L, false);

        assertThat(connectedUserConnectionService.getSharedConnectionIds()).containsExactly(6L);
    }
}

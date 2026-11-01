/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestConfiguration;
import com.bytechef.ee.embedded.execution.config.EmbeddedExecutionIntTestSupport;
import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper.ConnectedUserConnection;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 */
@SpringBootTest(classes = EmbeddedExecutionIntTestConfiguration.class)
class ConnectionIdHelperIntTest {

    @Autowired
    private ConnectionIdHelper connectionIdHelper;

    @Autowired
    private EmbeddedExecutionIntTestSupport embeddedExecutionIntTestSupport;

    @AfterEach
    void afterEach() {
        embeddedExecutionIntTestSupport.deleteAll();
    }

    @Test
    void testGetConnectedUserConnectionRejectsForeignInstance() {
        embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        long otherConnectedUserId = embeddedExecutionIntTestSupport.createConnectedUser(
            "user-2", Environment.PRODUCTION);

        IntegrationInstance foreignIntegrationInstance = embeddedExecutionIntTestSupport.createIntegrationInstance(
            otherConnectedUserId, "slack", Environment.PRODUCTION, 42L);

        assertThatThrownBy(
            () -> connectionIdHelper.getConnectedUserConnection(
                "user-1", "slack", foreignIntegrationInstance.getId(), Environment.PRODUCTION))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testGetConnectedUserConnectionReturnsConnectionForOwnedInstance() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        IntegrationInstance integrationInstance = embeddedExecutionIntTestSupport.createIntegrationInstance(
            connectedUserId, "slack", Environment.PRODUCTION, 42L);

        ConnectedUserConnection connectedUserConnection = connectionIdHelper.getConnectedUserConnection(
            "user-1", "slack", integrationInstance.getId(), Environment.PRODUCTION);

        assertThat(connectedUserConnection)
            .isEqualTo(new ConnectedUserConnection(connectedUserId, integrationInstance.getId(), 42L));
    }

    @Test
    void testGetConnectedUserConnectionResolvesTheConnectedUserInstanceOfTheComponent() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        embeddedExecutionIntTestSupport.createIntegrationInstance(
            connectedUserId, "github", Environment.PRODUCTION, 41L);

        IntegrationInstance integrationInstance = embeddedExecutionIntTestSupport.createIntegrationInstance(
            connectedUserId, "slack", Environment.PRODUCTION, 43L);

        ConnectedUserConnection connectedUserConnection = connectionIdHelper.getConnectedUserConnection(
            "user-1", "slack", null, Environment.PRODUCTION);

        assertThat(connectedUserConnection)
            .isEqualTo(new ConnectedUserConnection(connectedUserId, integrationInstance.getId(), 43L));
    }

    @Test
    void testGetConnectedUserConnectionKeepsTheConnectedUserWhenTheComponentHasNoInstance() {
        long connectedUserId = embeddedExecutionIntTestSupport.createConnectedUser("user-1", Environment.PRODUCTION);

        embeddedExecutionIntTestSupport.createIntegrationInstance(
            connectedUserId, "github", Environment.PRODUCTION, 41L);

        ConnectedUserConnection connectedUserConnection = connectionIdHelper.getConnectedUserConnection(
            "user-1", "slack", null, Environment.PRODUCTION);

        assertThat(connectedUserConnection).isEqualTo(new ConnectedUserConnection(connectedUserId, null, null));
    }
}

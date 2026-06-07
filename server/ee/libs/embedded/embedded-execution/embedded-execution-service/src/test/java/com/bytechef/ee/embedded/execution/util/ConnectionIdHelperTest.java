/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 */
class ConnectionIdHelperTest {

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final ConnectionIdHelper connectionIdHelper =
        new ConnectionIdHelper(connectedUserService, integrationInstanceService);

    @Test
    void testGetConnectionIdRejectsForeignInstance() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectedUserId()).thenReturn(99L);
        when(integrationInstanceService.getIntegrationInstance(5L)).thenReturn(integrationInstance);

        assertThrows(
            AccessDeniedException.class,
            () -> connectionIdHelper.getConnectionId("user-1", "slack", 5L, Environment.PRODUCTION));
    }

    @Test
    void testGetConnectionIdReturnsConnectionForOwnedInstance() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(1L);
        when(connectedUserService.getConnectedUser("user-1", Environment.PRODUCTION)).thenReturn(connectedUser);

        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectedUserId()).thenReturn(1L);
        when(integrationInstance.getConnectionId()).thenReturn(42L);
        when(integrationInstanceService.getIntegrationInstance(5L)).thenReturn(integrationInstance);

        Long connectionId = connectionIdHelper.getConnectionId("user-1", "slack", 5L, Environment.PRODUCTION);

        assertEquals(42L, connectionId);
    }
}

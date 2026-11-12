/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceConnectionCleanupTest {

    private static final long CONNECTED_USER_ID = 1L;
    private static final long CONNECTION_ID = 2L;

    private final ConnectedUserConnectionService connectedUserConnectionService = mock(
        ConnectedUserConnectionService.class);
    private final ConnectionFacade connectionFacade = mock(ConnectionFacade.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final TriggerSubscriptionService triggerSubscriptionService = mock(TriggerSubscriptionService.class);

    private final IntegrationInstanceConnectionCleanup integrationInstanceConnectionCleanup =
        new IntegrationInstanceConnectionCleanup(
            connectedUserConnectionService, connectionFacade, connectionService, integrationInstanceService,
            triggerSubscriptionService);

    @Test
    void testKeepsTheConnectionForTheConnectedUserWhenATriggerSubscriptionUsesIt() {
        when(triggerSubscriptionService.getConnectionTriggerSubscriptions(CONNECTION_ID))
            .thenReturn(List.of(new TriggerSubscription()));

        integrationInstanceConnectionCleanup.deleteOrKeepForConnectedUser(CONNECTED_USER_ID, CONNECTION_ID);

        verify(connectionService, never()).delete(anyLong());
        verify(connectedUserConnectionService).create(CONNECTED_USER_ID, CONNECTION_ID);
    }

    @Test
    void testDeletesAnUnusedConnection() {
        when(triggerSubscriptionService.getConnectionTriggerSubscriptions(CONNECTION_ID))
            .thenReturn(List.of());

        integrationInstanceConnectionCleanup.deleteOrKeepForConnectedUser(CONNECTED_USER_ID, CONNECTION_ID);

        verify(connectionService).delete(CONNECTION_ID);
        verify(connectedUserConnectionService, never()).create(anyLong(), anyLong());
    }
}

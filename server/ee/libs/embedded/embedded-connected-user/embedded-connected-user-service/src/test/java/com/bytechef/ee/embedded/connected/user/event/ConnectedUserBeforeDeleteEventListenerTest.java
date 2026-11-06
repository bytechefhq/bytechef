/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayConnectedUserSettingsService;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * @version ee
 * @author Ivica Cardic
 */
class ConnectedUserBeforeDeleteEventListenerTest {

    private static final long CONNECTED_USER_ID = 42L;

    private final AiGatewayConnectedUserSettingsService aiGatewayConnectedUserSettingsService =
        mock(AiGatewayConnectedUserSettingsService.class);
    private final AiGatewayProviderService aiGatewayProviderService = mock(AiGatewayProviderService.class);

    private final ConnectedUserBeforeDeleteEventListener connectedUserBeforeDeleteEventListener =
        new ConnectedUserBeforeDeleteEventListener(aiGatewayConnectedUserSettingsService, aiGatewayProviderService);

    @Test
    void testDeletingAConnectedUserDeletesItsSettingsRow() {
        connectedUserBeforeDeleteEventListener.onBeforeDelete(event());

        verify(aiGatewayConnectedUserSettingsService).deleteByConnectedUserId(CONNECTED_USER_ID);
    }

    @Test
    void testDeletingAConnectedUserDisablesItsProviders() {
        connectedUserBeforeDeleteEventListener.onBeforeDelete(event());

        verify(aiGatewayProviderService).disableByConnectedUserId(CONNECTED_USER_ID);
    }

    @SuppressWarnings("unchecked")
    private static BeforeDeleteEvent<ConnectedUser> event() {
        BeforeDeleteEvent<ConnectedUser> beforeDeleteEvent = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);

        when(beforeDeleteEvent.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(CONNECTED_USER_ID);

        return beforeDeleteEvent;
    }
}

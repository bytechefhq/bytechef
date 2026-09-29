/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectionApiControllerTest {

    private final ConnectedUserConnectionFacade connectedUserConnectionFacade =
        mock(ConnectedUserConnectionFacade.class);
    private final ConnectionFacade connectionFacade = mock(ConnectionFacade.class);
    private final ConversionService conversionService = mock(ConversionService.class);

    private ConnectionApiController connectionApiController;

    @BeforeEach
    void beforeEach() {
        connectionApiController = new ConnectionApiController(
            connectedUserConnectionFacade, connectionFacade, conversionService);
    }

    @Test
    void testGetConnectedUserConnectionsObfuscatesAuthorizationParametersByKey() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .componentName("slack")
            .id(20L)
            .build();

        Map<String, Object> authorizationParameters = new HashMap<>();

        authorizationParameters.put("clientId", "client-identifier");
        authorizationParameters.put("password", "supersecretpassword");
        authorizationParameters.put("token", "abcdefghijklmnopqrstuvwxyz");

        when(connectedUserConnectionFacade.getConnectedUserConnections(1L, "slack"))
            .thenReturn(List.of(connectionDTO));
        when(conversionService.convert(connectionDTO, ConnectionModel.class))
            .thenReturn(new ConnectionModel().authorizationParameters(authorizationParameters));

        List<ConnectionModel> connectionModels = connectionApiController.getConnectedUserConnections(
            1L, "slack", List.of())
            .getBody();

        assertThat(connectionModels).hasSize(1);

        ConnectionModel connectionModel = connectionModels.getFirst();

        assertThat(connectionModel.getAuthorizationParameters())
            .containsEntry("clientId", "client-identifier")
            .containsEntry("password", ".".repeat(28))
            .containsEntry("token", ".".repeat(28) + "stuvwxyz");
    }
}

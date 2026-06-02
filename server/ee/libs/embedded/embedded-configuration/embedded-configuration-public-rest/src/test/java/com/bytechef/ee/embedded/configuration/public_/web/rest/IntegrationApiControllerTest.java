/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationApiControllerTest {

    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade =
        mock(ConnectedUserIntegrationFacade.class);
    private final ConversionService conversionService = mock(ConversionService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final IntegrationApiController integrationApiController = new IntegrationApiController(
        conversionService, connectedUserIntegrationFacade, environmentService);

    @Test
    void testGetIntegrationReturnsNotFoundWhenIntegrationNotVisible() {
        when(environmentService.getEnvironment(any()))
            .thenReturn(Environment.PRODUCTION);
        when(connectedUserIntegrationFacade.getConnectedUserIntegration(
            anyString(), anyLong(), anyBoolean(), any()))
                .thenThrow(new EmbeddedIntegrationNotVisibleException(1L));

        ResponseEntity<IntegrationModel> responseEntity = integrationApiController.getIntegration(
            "external-user-id", 1L, EnvironmentModel.PRODUCTION);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetIntegrationReturnsOkWhenIntegrationVisible() {
        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = mock(ConnectedUserIntegrationDTO.class);
        IntegrationModel integrationModel = new IntegrationModel();

        when(environmentService.getEnvironment(any()))
            .thenReturn(Environment.PRODUCTION);
        when(connectedUserIntegrationFacade.getConnectedUserIntegration(
            anyString(), anyLong(), anyBoolean(), any()))
                .thenReturn(connectedUserIntegrationDTO);
        when(conversionService.convert(eq(connectedUserIntegrationDTO), eq(IntegrationModel.class)))
            .thenReturn(integrationModel);

        ResponseEntity<IntegrationModel> responseEntity = integrationApiController.getIntegration(
            "external-user-id", 1L, EnvironmentModel.PRODUCTION);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isSameAs(integrationModel);
    }
}

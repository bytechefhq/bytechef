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
import com.bytechef.platform.security.web.authentication.ConnectedUserPathBindings;
import com.bytechef.platform.security.web.authentication.ConnectedUserPathBindings.ExternalUserIdEndpoint;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationApiControllerTest {

    private static final String BASE_PACKAGE = "com.bytechef.ee.embedded.configuration.public_.web.rest";

    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade =
        mock(ConnectedUserIntegrationFacade.class);
    private final ConversionService conversionService = mock(ConversionService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final IntegrationApiController integrationApiController = new IntegrationApiController(
        conversionService, connectedUserIntegrationFacade, environmentService);

    @BeforeEach
    void authenticateTheConnectedUser() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of("external-user-id"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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

    @Test
    void testDiscoversEveryExternalUserIdEndpoint() {
        assertThat(externalUserIdEndpoints()).hasSize(2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalUserIdEndpoints")
    void testAConnectedUserIsRefusedAnotherConnectedUsersPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        ConnectedUserPathBindings.assertRefusesAnotherConnectedUser(externalUserIdEndpoint);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalUserIdEndpoints")
    void testAPlatformSessionIsRefusedAConnectedUsersPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        ConnectedUserPathBindings.assertRefusesAPlatformSession(externalUserIdEndpoint);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("externalUserIdEndpoints")
    void testAConnectedUserPassesTheGuardOnTheirOwnPath(ExternalUserIdEndpoint externalUserIdEndpoint)
        throws ReflectiveOperationException {

        ConnectedUserPathBindings.assertLetsTheConnectedUserActOnTheirOwnPath(externalUserIdEndpoint);
    }

    static List<ExternalUserIdEndpoint> externalUserIdEndpoints() {
        List<ExternalUserIdEndpoint> externalUserIdEndpoints =
            ConnectedUserPathBindings.findExternalUserIdEndpoints(BASE_PACKAGE);

        return externalUserIdEndpoints.stream()
            .filter(
                externalUserIdEndpoint -> externalUserIdEndpoint.controllerClass() == IntegrationApiController.class)
            .toList();
    }
}

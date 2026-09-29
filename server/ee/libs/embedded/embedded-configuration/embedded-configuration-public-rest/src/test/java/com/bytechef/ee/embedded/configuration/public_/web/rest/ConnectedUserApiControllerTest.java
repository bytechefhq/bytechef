/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.web.authentication.ConnectedUserPathBindings;
import com.bytechef.platform.security.web.authentication.ConnectedUserPathBindings.ExternalUserIdEndpoint;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserApiControllerTest {

    private static final String BASE_PACKAGE = "com.bytechef.ee.embedded.configuration.public_.web.rest";

    private static final String EXTERNAL_USER_ID = "alice";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final ConnectedUserApiController connectedUserApiController = new ConnectedUserApiController(
        connectedUserService, environmentService);

    @BeforeEach
    void setUp() {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEmail("alice@example.com");
        connectedUser.setExternalId(EXTERNAL_USER_ID);
        connectedUser.setMetadata(Map.of("plan", "free"));
        connectedUser.setName("Alice");

        when(environmentService.getEnvironment(any())).thenReturn(Environment.PRODUCTION);
        when(connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUser);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @MethodSource("visibilityAttributeChanges")
    void testAConnectedUserTokenCannotChangeItsOwnVisibilityAttributesThroughMe(Map<String, Object> requestBody) {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        assertThatThrownBy(
            () -> connectedUserApiController.updateFrontendConnectedUser(EnvironmentModel.PRODUCTION, requestBody))
                .isInstanceOf(AccessDeniedException.class);

        verify(connectedUserService, never()).updateConnectedUser(anyString(), any(), anyMap());
    }

    @ParameterizedTest
    @MethodSource("visibilityAttributeChanges")
    void testAConnectedUserTokenCannotChangeItsOwnVisibilityAttributesThroughItsPath(
        Map<String, Object> requestBody) {

        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        assertThatThrownBy(
            () -> connectedUserApiController.updateConnectedUser(
                EXTERNAL_USER_ID, EnvironmentModel.PRODUCTION, requestBody))
                    .isInstanceOf(AccessDeniedException.class);

        verify(connectedUserService, never()).updateConnectedUser(anyString(), any(), anyMap());
    }

    @Test
    void testAConnectedUserTokenMaySendUnchangedValues() {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.of(EXTERNAL_USER_ID));

        Map<String, Object> requestBody = Map.of("email", "alice@example.com", "name", "Alice", "plan", "free");

        ResponseEntity<Void> responseEntity = connectedUserApiController.updateFrontendConnectedUser(
            EnvironmentModel.PRODUCTION, requestBody);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(connectedUserService).updateConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION, requestBody);
    }

    @ParameterizedTest
    @MethodSource("visibilityAttributeChanges")
    void testTheApiKeyPrincipalUpdatesVisibilityAttributes(Map<String, Object> requestBody) {
        SecurityContextHolder.getContext()
            .setAuthentication(TestConnectedUserAuthentication.apiKey(EXTERNAL_USER_ID));

        connectedUserApiController.updateConnectedUser(EXTERNAL_USER_ID, EnvironmentModel.PRODUCTION, requestBody);
        connectedUserApiController.updateFrontendConnectedUser(EnvironmentModel.PRODUCTION, requestBody);

        verify(connectedUserService, times(2))
            .updateConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION, requestBody);
    }

    static List<Map<String, Object>> visibilityAttributeChanges() {
        return List.of(
            Map.of("plan", "enterprise"), Map.of("tier", "gold"), Map.of("NAME", "Mallory"),
            Map.of("email", "mallory@example.com"));
    }

    @Test
    void testDiscoversEveryExternalUserIdEndpoint() {
        assertThat(externalUserIdEndpoints()).hasSize(1);
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
                externalUserIdEndpoint -> externalUserIdEndpoint.controllerClass() == ConnectedUserApiController.class)
            .toList();
    }
}

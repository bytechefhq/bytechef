/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserConnectionFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.AbstractException;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedConfigurationPublicRestTestConfiguration.class)
@Import({
    ConnectionApiControllerFrontendIntTest.TestNoSuchElementExceptionAdvice.class,
    ConnectionApiControllerFrontendIntTest.TestAbstractExceptionAdvice.class
})
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(ConnectionApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class ConnectionApiControllerFrontendIntTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final long CONNECTED_USER_ID = 7L;

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ConnectedUserConnectionFacade connectedUserConnectionFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);
        when(connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUser);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testGetAllFrontendConnectionsListsEveryComponent() {
        ConnectionDTO slackConnection = ConnectionDTO.builder()
            .id(1L)
            .componentName("slack")
            .name("Slack")
            .connectionVersion(2)
            .authorizationType(AuthorizationType.API_KEY)
            .createdDate(Instant.parse("2026-01-01T00:00:00Z"))
            .build();
        ConnectionDTO hubspotConnection = ConnectionDTO.builder()
            .id(2L)
            .componentName("hubspot")
            .name("Hubspot")
            .build();

        when(connectedUserConnectionFacade.getConnections(CONNECTED_USER_ID, null, List.of()))
            .thenReturn(List.of(slackConnection, hubspotConnection));

        webTestClient
            .get()
            .uri("/v1/connections")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(2)
            .jsonPath("$[0].componentName")
            .isEqualTo("slack")
            .jsonPath("$[0].connectionVersion")
            .isEqualTo(2)
            .jsonPath("$[0].authorizationType")
            .isEqualTo("API_KEY")
            .jsonPath("$[0].createdDate")
            .isNotEmpty();
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCreateFrontendConnectionReturnsId() {
        when(connectedUserConnectionFacade.createConnectedUserConnection(eq(CONNECTED_USER_ID), any()))
            .thenReturn(42L);

        webTestClient
            .post()
            .uri("/v1/components/{componentName}/connections", "slack")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                    {"name":"My Slack","authorizationType":"OAUTH2_AUTHORIZATION_CODE","connectionVersion":1,\
                    "parameters":{"code":"abc"}}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$")
            .isEqualTo(42);

        ArgumentCaptor<ConnectionDTO> captor = ArgumentCaptor.forClass(ConnectionDTO.class);

        verify(connectedUserConnectionFacade).createConnectedUserConnection(eq(CONNECTED_USER_ID), captor.capture());

        ConnectionDTO capturedConnectionDTO = captor.getValue();

        assertThat(capturedConnectionDTO.componentName()).isEqualTo("slack");
        assertThat(capturedConnectionDTO.name()).isEqualTo("My Slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDeleteFrontendConnectionSucceeds() {
        webTestClient
            .delete()
            .uri("/v1/connections/{id}", 5L)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserConnectionFacade).deleteConnectedUserConnection(CONNECTED_USER_ID, 5L);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDeleteFrontendConnectionMapsInUseTo409() {
        doThrow(new ConfigurationException("used", ConnectionErrorType.CONNECTION_IS_USED))
            .when(connectedUserConnectionFacade)
            .deleteConnectedUserConnection(CONNECTED_USER_ID, 5L);

        webTestClient
            .delete()
            .uri("/v1/connections/{id}", 5L)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.reason")
            .isEqualTo("CONNECTION_IS_USED");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDeleteFrontendConnectionForeignReturns404() {
        doThrow(new NoSuchElementException("Connection id=5 not found"))
            .when(connectedUserConnectionFacade)
            .deleteConnectedUserConnection(CONNECTED_USER_ID, 5L);

        webTestClient
            .delete()
            .uri("/v1/connections/{id}", 5L)
            .exchange()
            .expectStatus()
            .isEqualTo(404);
    }

    /**
     * Regression for a controller-local {@code @ExceptionHandler(ConfigurationException.class)} that used to intercept
     * EVERY {@link ConfigurationException} thrown from this controller (a local handler outranks
     * {@code @ControllerAdvice}), rethrowing anything that was not {@code CONNECTION_IS_USED} — which
     * {@code ExceptionHandlerExceptionResolver} does not re-dispatch to the next resolver, so the rethrow degraded to a
     * bare 500 instead of the platform-wide 400 {@code ProblemDetail}. A non-{@code CONNECTION_IS_USED}
     * {@link ConfigurationException} must keep its 400, unmodified by anything in this controller.
     */
    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDeleteFrontendConnectionOtherConfigurationExceptionReturns400() {
        doThrow(new ConfigurationException("invalid", ConnectionErrorType.INVALID_CONNECTION))
            .when(connectedUserConnectionFacade)
            .deleteConnectedUserConnection(CONNECTED_USER_ID, 5L);

        webTestClient
            .delete()
            .uri("/v1/connections/{id}", 5L)
            .exchange()
            .expectStatus()
            .isEqualTo(400);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testReauthorizeFrontendConnection() {
        webTestClient
            .post()
            .uri("/v1/connections/{id}/reauthorize", 5L)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"parameters":{"apiKey":"x"}}""")
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserConnectionFacade)
            .reauthorizeConnectedUserConnection(CONNECTED_USER_ID, 5L, Map.of("apiKey", "x"));
    }

    /**
     * This module depends on {@code rest-api} only, not {@code rest-impl}, so the production
     * {@code GlobalResponseEntityExceptionHandler} that maps {@link NoSuchElementException} to 404 in the real running
     * app is not reachable from this WebMvcTest slice. This test-only stand-in mirrors just that one mapping so
     * {@link #testDeleteFrontendConnectionForeignReturns404} exercises the same HTTP contract the production advice
     * provides, without pulling the whole rest-impl module into this module's dependencies.
     */
    @RestControllerAdvice
    static class TestNoSuchElementExceptionAdvice {

        @ExceptionHandler(NoSuchElementException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public void handleNoSuchElementException() {
        }
    }

    /**
     * Mirrors {@code GlobalResponseEntityExceptionHandler#handleAbstractException}'s {@link AbstractException} to 400
     * mapping (same reachability gap as {@link TestNoSuchElementExceptionAdvice} — this module doesn't depend on
     * {@code rest-impl}), so {@link #testDeleteFrontendConnectionOtherConfigurationExceptionReturns400} can prove a
     * non-{@code CONNECTION_IS_USED} {@link ConfigurationException} keeps its 400 instead of being caught (and
     * potentially mis-mapped) by anything declared on {@link ConnectionApiController} itself.
     */
    @RestControllerAdvice
    static class TestAbstractExceptionAdvice {

        @ExceptionHandler(AbstractException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public void handleAbstractException() {
        }
    }
}

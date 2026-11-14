/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedConfigurationPublicRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(IntegrationInstanceApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class IntegrationInstanceApiControllerIntTest {

    private static final String EXTERNAL_USER_ID = "external-user-id";
    private static final long INTEGRATION_ID = 1L;
    private static final long INTEGRATION_INSTANCE_ID = 7L;

    @MockitoBean
    private AutomationProjectFacade automationProjectFacade;

    @Autowired
    private ConnectedUserIntegrationFacade connectedUserIntegrationFacade;

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

        when(environmentService.getEnvironment("PRODUCTION")).thenReturn(Environment.PRODUCTION);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCreateFrontendIntegrationInstanceReturnsNotFoundWhenIntegrationNotVisible() {
        when(connectedUserIntegrationFacade.createIntegrationInstance(
            EXTERNAL_USER_ID, INTEGRATION_ID, Map.of(), Environment.PRODUCTION))
                .thenThrow(new EmbeddedIntegrationNotVisibleException(INTEGRATION_ID));

        webTestClient
            .post()
            .uri("/v1/integrations/{id}/instances", INTEGRATION_ID)
            .header("X-Environment", "PRODUCTION")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"connection":{"parameters":{}}}""")
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCreateFrontendIntegrationInstanceReturnsTheInstanceIdWhenIntegrationVisible() {
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getId()).thenReturn(INTEGRATION_INSTANCE_ID);
        when(connectedUserIntegrationFacade.createIntegrationInstance(
            EXTERNAL_USER_ID, INTEGRATION_ID, Map.of(), Environment.PRODUCTION))
                .thenReturn(integrationInstance);

        webTestClient
            .post()
            .uri("/v1/integrations/{id}/instances", INTEGRATION_ID)
            .header("X-Environment", "PRODUCTION")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"connection":{"parameters":{}}}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$")
            .isEqualTo(INTEGRATION_INSTANCE_ID);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCreateIntegrationInstanceReturnsNotFoundWhenIntegrationNotVisible() {
        when(connectedUserIntegrationFacade.createIntegrationInstance(
            EXTERNAL_USER_ID, INTEGRATION_ID, Map.of(), Environment.PRODUCTION))
                .thenThrow(new EmbeddedIntegrationNotVisibleException(INTEGRATION_ID));

        webTestClient
            .post()
            .uri("/v1/{externalUserId}/integrations/{id}/instances", EXTERNAL_USER_ID, INTEGRATION_ID)
            .header("X-Environment", "PRODUCTION")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"connection":{"parameters":{}}}""")
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}

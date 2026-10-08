/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
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
@WebMvcTest(IntegrationApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class IntegrationApiControllerIntTest {

    private static final String EXTERNAL_USER_ID = "external-user-id";
    private static final long INTEGRATION_ID = 1L;
    private static final String INTEGRATION_PATH = "/v1/{externalUserId}/integrations/{id}";

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

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
    void testGetIntegrationReturnsNotFoundWhenIntegrationNotVisible() {
        when(connectedUserIntegrationFacade.getConnectedUserIntegration(
            EXTERNAL_USER_ID, INTEGRATION_ID, true, Environment.PRODUCTION))
                .thenThrow(new EmbeddedIntegrationNotVisibleException(INTEGRATION_ID));

        webTestClient
            .get()
            .uri(INTEGRATION_PATH, EXTERNAL_USER_ID, INTEGRATION_ID)
            .header("X-Environment", "PRODUCTION")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();

        verify(connectedUserIntegrationFacade)
            .getConnectedUserIntegration(EXTERNAL_USER_ID, INTEGRATION_ID, true, Environment.PRODUCTION);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testGetIntegrationReturnsOkWhenIntegrationVisible() {
        IntegrationDTO integrationDTO = new IntegrationDTO(
            null, "slack", 1, null, null, "Slack integration", "slack-icon", INTEGRATION_ID, List.of(), List.of(),
            null, null, null, null, 2, true, "slack", null, List.of(), "Slack", 0);

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            IntegrationInstanceConfigurationDTO.builder()
                .integration(integrationDTO)
                .integrationId(INTEGRATION_ID)
                .integrationVersion(2)
                .integrationInstanceConfigurationWorkflows(List.of())
                .build();

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = new ConnectedUserIntegrationDTO(
            null, integrationInstanceConfigurationDTO, List.of(), null, null, List.of(), List.of());

        when(connectedUserIntegrationFacade.getConnectedUserIntegration(
            EXTERNAL_USER_ID, INTEGRATION_ID, true, Environment.PRODUCTION))
                .thenReturn(connectedUserIntegrationDTO);

        webTestClient
            .get()
            .uri(INTEGRATION_PATH, EXTERNAL_USER_ID, INTEGRATION_ID)
            .header("X-Environment", "PRODUCTION")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(INTEGRATION_ID)
            .jsonPath("$.componentName")
            .isEqualTo("slack")
            .jsonPath("$.name")
            .isEqualTo("Slack")
            .jsonPath("$.description")
            .isEqualTo("Slack integration")
            .jsonPath("$.icon")
            .isEqualTo("slack-icon")
            .jsonPath("$.integrationVersion")
            .isEqualTo(2)
            .jsonPath("$.multipleInstances")
            .isEqualTo(true);

        verify(connectedUserIntegrationFacade)
            .getConnectedUserIntegration(EXTERNAL_USER_ID, INTEGRATION_ID, true, Environment.PRODUCTION);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO.ProvisionableIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.facade.AppEventFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceAdminFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestTestConfiguration;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = EmbeddedConfigurationRestTestConfiguration.class)
@WebMvcTest(IntegrationInstanceApiController.class)
@EmbeddedConfigurationRestConfigurationSharedMocks
class IntegrationInstanceApiControllerIntTest {

    @MockitoBean
    private AppEventFacade appEventFacade;

    @MockitoBean
    private AppEventService appEventService;

    @MockitoBean
    private ComponentConnectionFacade componentConnectionFacade;

    @MockitoBean
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @MockitoBean
    private IntegrationFacade integrationFacade;

    @MockitoBean
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @MockitoBean
    private IntegrationInstanceFacade integrationInstanceFacade;

    @MockitoBean
    private IntegrationInstanceService integrationInstanceService;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private IntegrationWorkflowFacade integrationWorkflowFacade;

    @MockitoBean
    private WorkflowFacade workflowFacade;

    @MockitoBean
    private WorkflowService workflowService;

    @Autowired
    private IntegrationInstanceAdminFacade integrationInstanceAdminFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    void testProvisionIntegrationInstancePassesBodyToTheAdminFacade() {
        when(integrationInstanceAdminFacade.provisionIntegrationInstance(eq(5L), eq(9L), eq(Map.of("token", "t")),
            any()))
                .thenReturn(123L);

        this.webTestClient
            .post()
            .uri("/internal/connected-users/5/integration-instances")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                    {"integrationInstanceConfigurationId":9,"connectionParameters":{"token":"t"},
                     "workflows":[{"workflowUuid":"u-1","inputs":{"channel":"#ops"}}]}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Long.class)
            .isEqualTo(123L);

        ArgumentCaptor<List<ProvisionIntegrationInstanceWorkflowDTO>> workflowsCaptor = ArgumentCaptor.captor();

        verify(integrationInstanceAdminFacade)
            .provisionIntegrationInstance(eq(5L), eq(9L), any(), workflowsCaptor.capture());

        assertThat(workflowsCaptor.getValue())
            .containsExactly(new ProvisionIntegrationInstanceWorkflowDTO("u-1", Map.of("channel", "#ops")));
    }

    @Test
    void testProvisionIntegrationInstanceTreatsExplicitNullsAsEmpty() {
        when(integrationInstanceAdminFacade.provisionIntegrationInstance(eq(5L), eq(9L), any(), any()))
            .thenReturn(124L);

        this.webTestClient
            .post()
            .uri("/internal/connected-users/5/integration-instances")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                    {"integrationInstanceConfigurationId":9,"connectionParameters":null,"workflows":null}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(Long.class)
            .isEqualTo(124L);

        verify(integrationInstanceAdminFacade).provisionIntegrationInstance(5L, 9L, Map.of(), List.of());
    }

    @Test
    void testGetProvisionableIntegrationsMapsSelfServeAndSharedParameterNames() {
        when(integrationInstanceAdminFacade.getProvisionableIntegrations(5L))
            .thenReturn(List.of(getProvisionableIntegration()));

        this.webTestClient
            .get()
            .uri("/internal/connected-users/5/provisionable-integrations")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].integration.componentName")
            .isEqualTo("slack")
            .jsonPath("$[0].componentVersion")
            .isEqualTo(2)
            .jsonPath("$[0].integrationInstanceConfigurations[0].integrationInstanceConfiguration.id")
            .isEqualTo(9)
            .jsonPath("$[0].integrationInstanceConfigurations[0].integrationInstanceConfiguration.connectionParameters")
            .doesNotExist()
            .jsonPath("$[0].integrationInstanceConfigurations[0].selfServe")
            .isEqualTo(true)
            .jsonPath("$[0].integrationInstanceConfigurations[0].sharedConnectionParameterNames[0]")
            .isEqualTo("clientId")
            .jsonPath("$[0].integrationInstanceConfigurations[0].oAuth2AuthorizationParameters.authorizationUrl")
            .isEqualTo("https://auth.example.com")
            .jsonPath("$[0].integrationInstanceConfigurations[0].oAuth2AuthorizationParameters.scopes.read")
            .isEqualTo(true)
            .jsonPath("$[0].integrationInstanceConfigurations[0].redirectUri")
            .isEqualTo("https://app.example.com/callback");
    }

    private static ProvisionableIntegrationDTO getProvisionableIntegration() {
        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .componentName("slack")
            .id(3L)
            .name("slack")
            .tags(List.of())
            .build();

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO = IntegrationInstanceConfigurationDTO
            .builder()
            .authorizationType(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .connectionParameters(Map.of("clientSecret", "super-secret"))
            .enabled(true)
            .environment(Environment.PRODUCTION)
            .id(9L)
            .integrationId(3L)
            .name("config")
            .tags(List.of())
            .build();

        return new ProvisionableIntegrationDTO(
            integrationDTO, 2,
            List.of(
                new ProvisionableIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationDTO, true, List.of("clientId"),
                    new OAuth2AuthorizationParameters(
                        "https://auth.example.com", "client-id", Map.of(), Map.of("read", true)),
                    "https://app.example.com/callback")));
    }
}

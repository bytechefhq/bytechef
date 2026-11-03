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
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectConfigurationVersionImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationConnectedUserDTO;
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
@WebMvcTest({
    IntegrationInstanceApiController.class, IntegrationInstanceConfigurationApiController.class
})
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

    @Test
    void testGetIntegrationInstanceConfigurationConnectedUsersMapsTheConnectedUsers() {
        when(integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationConnectedUsers(5L))
            .thenReturn(
                List.of(
                    new IntegrationInstanceConfigurationConnectedUserDTO(
                        7L, "external-7", "Alice", "alice@example.com")));

        this.webTestClient
            .get()
            .uri("/internal/integration-instance-configurations/5/connected-users")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].connectedUserId")
            .isEqualTo(7)
            .jsonPath("$[0].externalId")
            .isEqualTo("external-7")
            .jsonPath("$[0].name")
            .isEqualTo("Alice")
            .jsonPath("$[0].email")
            .isEqualTo("alice@example.com");
    }

    @Test
    void testGetIntegrationInstanceConfigurationMoveImpactPassesBodyToTheFacade() {
        when(integrationInstanceConfigurationFacade.getMoveImpact(5L, 6L, List.of(1L, 2L)))
            .thenReturn(
                new ConfigurationMoveImpactDTO(
                    List.of(
                        new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-1", "Cron", List.of("cronExpression"), List.of("slack"), 1)),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            2L, ConfigurationMoveResultDTO.Skipped.CONNECTION_MISMATCH, List.of()))));

        this.webTestClient
            .post()
            .uri("/internal/integration-instance-configurations/5/move-impact")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"targetId":6,"connectedUserIds":[1,2]}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.workflows[0].workflowUuid")
            .isEqualTo("uuid-1")
            .jsonPath("$.workflows[0].newRequiredInputs[0]")
            .isEqualTo("cronExpression")
            .jsonPath("$.workflows[0].newRequiredConnections[0]")
            .isEqualTo("slack")
            .jsonPath("$.workflows[0].affectedConnectedUserCount")
            .isEqualTo(1)
            .jsonPath("$.skipped[0].connectedUserId")
            .isEqualTo(2)
            .jsonPath("$.skipped[0].reason")
            .isEqualTo("CONNECTION_MISMATCH");
    }

    @Test
    void testMoveIntegrationInstanceConfigurationConnectedUsersPassesBodyToTheFacade() {
        when(integrationInstanceConfigurationFacade.moveConnectedUsers(5L, 6L, List.of(1L, 2L)))
            .thenReturn(
                new ConfigurationMoveResultDTO(
                    List.of(1L),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            2L, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_ENABLED_WORKFLOWS,
                            List.of("Cron"))),
                    List.of(1L)));

        this.webTestClient
            .post()
            .uri("/internal/integration-instance-configurations/5/move")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"targetId":6,"connectedUserIds":[1,2]}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.movedConnectedUserIds[0]")
            .isEqualTo(1)
            .jsonPath("$.needsAttentionConnectedUserIds[0]")
            .isEqualTo(1)
            .jsonPath("$.skipped[0].connectedUserId")
            .isEqualTo(2)
            .jsonPath("$.skipped[0].reason")
            .isEqualTo("TARGET_LACKS_ENABLED_WORKFLOWS")
            .jsonPath("$.skipped[0].workflowLabels[0]")
            .isEqualTo("Cron");

        verify(integrationInstanceConfigurationFacade).moveConnectedUsers(5L, 6L, List.of(1L, 2L));
    }

    @Test
    void testGetIntegrationInstanceConfigurationUpgradeImpactPassesBodyToTheFacade() {
        when(integrationInstanceConfigurationFacade.getUpgradeImpact(7L, List.of(1L, 2L)))
            .thenReturn(
                new ConfigurationMoveImpactDTO(
                    List.of(
                        new AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow(
                            "uuid-1", "Cron", List.of("cronExpression"), List.of("slack"), 1)),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            2L, ConfigurationMoveResultDTO.Skipped.CONNECTION_MISMATCH, List.of()))));

        this.webTestClient
            .post()
            .uri("/internal/integration-instance-configurations/7/upgrade-impact")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"connectedUserIds":[1,2]}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.workflows[0].workflowUuid")
            .isEqualTo("uuid-1")
            .jsonPath("$.workflows[0].newRequiredInputs[0]")
            .isEqualTo("cronExpression")
            .jsonPath("$.workflows[0].affectedConnectedUserCount")
            .isEqualTo(1)
            .jsonPath("$.skipped[0].connectedUserId")
            .isEqualTo(2)
            .jsonPath("$.skipped[0].reason")
            .isEqualTo("CONNECTION_MISMATCH");

        verify(integrationInstanceConfigurationFacade).getUpgradeImpact(7L, List.of(1L, 2L));
    }

    @Test
    void testUpgradeIntegrationInstanceConfigurationConnectedUsersPassesBodyToTheFacade() {
        when(integrationInstanceConfigurationFacade.upgradeConnectedUsers(7L, List.of(1L, 2L)))
            .thenReturn(
                new ConfigurationMoveResultDTO(
                    List.of(1L),
                    List.of(
                        new ConfigurationMoveResultDTO.Skipped(
                            2L, ConfigurationMoveResultDTO.Skipped.TARGET_LACKS_ENABLED_WORKFLOWS,
                            List.of("Cron"))),
                    List.of(1L)));

        this.webTestClient
            .post()
            .uri("/internal/integration-instance-configurations/7/upgrade")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"connectedUserIds":[1,2]}""")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.movedConnectedUserIds[0]")
            .isEqualTo(1)
            .jsonPath("$.needsAttentionConnectedUserIds[0]")
            .isEqualTo(1)
            .jsonPath("$.skipped[0].connectedUserId")
            .isEqualTo(2)
            .jsonPath("$.skipped[0].reason")
            .isEqualTo("TARGET_LACKS_ENABLED_WORKFLOWS");

        verify(integrationInstanceConfigurationFacade).upgradeConnectedUsers(7L, List.of(1L, 2L));
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

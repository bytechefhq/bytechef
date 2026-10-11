/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationWorkflowTemplateNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.ConnectionNotEntitledException;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.exception.MissingInputException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.configurer.EmbeddedApiKeySecurityConfigurer;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.security.service.ApiKeyService;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
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
@WebMvcTest(ConnectedUserProjectWorkflowApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class ConnectedUserProjectWorkflowApiControllerIntTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String HIDDEN_WORKFLOW_UUID = "hidden-automation-workflow-uuid";
    private static final String UNKNOWN_WORKFLOW_UUID = "no-such-uuid";
    private static final String WORKFLOW_UUID = "automation-workflow-uuid-1";

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        this.webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();

        when(environmentService.getEnvironment(any()))
            .thenReturn(Environment.PRODUCTION);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCopyFrontendWorkflowTemplateReturnsWorkflowUuid() {
        String newWorkflowUuid = "new-workflow-uuid-999";

        when(connectedUserProjectFacade.copyWorkflowTemplate(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class)))
                .thenReturn(newWorkflowUuid);

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/copy", WORKFLOW_UUID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(String.class)
            .isEqualTo("\"" + newWorkflowUuid + "\"");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCreateFrontendProjectWorkflowReturnsWorkflowUuidAsJsonString() {
        String newWorkflowUuid = "new-workflow-uuid-777";

        when(connectedUserProjectFacade.createProjectWorkflow(
            eq(EXTERNAL_USER_ID), eq("{}"), any(Environment.class)))
                .thenReturn(newWorkflowUuid);

        webTestClient
            .post()
            .uri("/v1/automation/workflows")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue("{\"definition\":\"{}\"}")
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String.class)
            .isEqualTo("\"" + newWorkflowUuid + "\"");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCopyFrontendWorkflowTemplateUnknownIdReturns404() {
        when(connectedUserProjectFacade.copyWorkflowTemplate(
            eq(EXTERNAL_USER_ID), eq(UNKNOWN_WORKFLOW_UUID), any(Environment.class)))
                .thenThrow(new IllegalArgumentException("Workflow template not found: " + UNKNOWN_WORKFLOW_UUID));

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/copy", UNKNOWN_WORKFLOW_UUID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testCopyFrontendWorkflowTemplateHiddenTemplateIsIndistinguishableFromUnknownUuid() {
        when(connectedUserProjectFacade.copyWorkflowTemplate(
            eq(EXTERNAL_USER_ID), eq(HIDDEN_WORKFLOW_UUID), any(Environment.class)))
                .thenThrow(
                    new IllegalArgumentException(
                        "Not a published automation workflow template: " + HIDDEN_WORKFLOW_UUID));
        when(connectedUserProjectFacade.copyWorkflowTemplate(
            eq(EXTERNAL_USER_ID), eq(UNKNOWN_WORKFLOW_UUID), any(Environment.class)))
                .thenThrow(
                    new IllegalArgumentException(
                        "Not a published automation workflow template: " + UNKNOWN_WORKFLOW_UUID));

        expectCopyNotFoundWithoutBody(HIDDEN_WORKFLOW_UUID);
        expectCopyNotFoundWithoutBody(UNKNOWN_WORKFLOW_UUID);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionUsesThePrincipalAsExternalUserId() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()), isNull()))
                .thenReturn(new ConnectedUserProjectWorkflow());

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade)
            .getOrCreateReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()),
                isNull());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionForwardsRequestedConnectionsFromTheBody() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of("slack", 12L)), isNull()))
                .thenReturn(new ConnectedUserProjectWorkflow());

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .bodyValue(Map.of("connections", Map.of("slack", 12)))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade).getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of("slack", 12L)), isNull());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionForwardsInputsFromTheBody() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of("slack", 12L)),
            eq(Map.of("channel", "#alerts", "limit", 5))))
                .thenReturn(new ConnectedUserProjectWorkflow());

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .bodyValue(
                Map.of("connections", Map.of("slack", 12), "inputs", Map.of("channel", "#alerts", "limit", 5)))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade).getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of("slack", 12L)),
            eq(Map.of("channel", "#alerts", "limit", 5)));
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionTreatsEmptyInputsAsNotGiven() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()), isNull()))
                .thenReturn(new ConnectedUserProjectWorkflow());

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .bodyValue(Map.of("inputs", Map.of()))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade).getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()), isNull());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionMissingConnectionReturns409() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(any(), any(), any(), any(), any()))
            .thenThrow(new MissingConnectionException("slack"));

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingConnectionComponentName")
            .isEqualTo("slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionConnectionNotEntitledReturns400() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(any(), any(), any(), any(), any()))
            .thenThrow(new ConnectionNotEntitledException("slack", 12L));

        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .bodyValue(Map.of("connections", Map.of("slack", 12)))
            .exchange()
            .expectStatus()
            .isBadRequest();
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionHiddenTemplateIsIndistinguishableFromUnknownUuid() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(HIDDEN_WORKFLOW_UUID), any(Environment.class), any(), any()))
                .thenThrow(
                    new AutomationWorkflowTemplateNotVisibleException(HIDDEN_WORKFLOW_UUID));
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(UNKNOWN_WORKFLOW_UUID), any(Environment.class), any(), any()))
                .thenThrow(
                    new AutomationWorkflowTemplateNotVisibleException(UNKNOWN_WORKFLOW_UUID));

        expectFrontendProvisionNotFoundWithoutBody(HIDDEN_WORKFLOW_UUID);
        expectFrontendProvisionNotFoundWithoutBody(UNKNOWN_WORKFLOW_UUID);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendDeprovisionDeletesTheReference() {
        webTestClient
            .delete()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade)
            .deleteReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testFrontendProvisionConnectionWithoutIdReturns400() {
        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", WORKFLOW_UUID)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"connections\":{\"slack\":null}}")
            .exchange()
            .expectStatus()
            .isBadRequest();

        verify(connectedUserWorkflowReferenceFacade, never())
            .getOrCreateReference(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExplicitProvisionCreatesAReference() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()), isNull()))
                .thenReturn(reference);

        webTestClient
            .post()
            .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade)
            .getOrCreateReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()),
                isNull());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExplicitProvisionMissingConnectionReturns409() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()), isNull()))
                .thenThrow(new MissingConnectionException("slack"));

        webTestClient
            .post()
            .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingConnectionComponentName")
            .isEqualTo("slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExplicitProvisionHiddenTemplateIsIndistinguishableFromUnknownUuid() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(HIDDEN_WORKFLOW_UUID), any(Environment.class), any(), any()))
                .thenThrow(
                    new AutomationWorkflowTemplateNotVisibleException(HIDDEN_WORKFLOW_UUID));
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(UNKNOWN_WORKFLOW_UUID), any(Environment.class), any(), any()))
                .thenThrow(
                    new AutomationWorkflowTemplateNotVisibleException(UNKNOWN_WORKFLOW_UUID));

        expectProvisionNotFoundWithoutBody(HIDDEN_WORKFLOW_UUID);
        expectProvisionNotFoundWithoutBody(UNKNOWN_WORKFLOW_UUID);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExplicitProvisionFailureOtherThanVisibilityIsNotReportedAsNotFound() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), any(), any()))
                .thenThrow(
                    new IllegalArgumentException(
                        "Automation workflow " + WORKFLOW_UUID + " is not in version 2 of project id=5"));

        Throwable thrown = catchThrowable(
            () -> mockMvc.perform(
                post(
                    "/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                    WORKFLOW_UUID)));

        assertThat(thrown).hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDeprovisionDeletesTheReference() {
        webTestClient
            .delete()
            .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade)
            .deleteReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testEnableReferenceMissingConnectionReturns409() {
        doThrow(new MissingConnectionException("slack"))
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());

        webTestClient
            .post()
            .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingConnectionComponentName")
            .isEqualTo("slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testEnableReferenceSucceeds() {
        doNothing()
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());

        webTestClient
            .post()
            .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDisableReferenceSucceeds() {
        doNothing()
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(false), any());

        webTestClient
            .delete()
            .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(false), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testDisableReferenceMissingConnectionReturns409() {
        doThrow(new MissingConnectionException("slack"))
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(false), any());

        webTestClient
            .delete()
            .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingConnectionComponentName")
            .isEqualTo("slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testEnableReferenceMissingInputReturns409() {
        doThrow(new MissingInputException("channel"))
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());

        webTestClient
            .post()
            .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingInputName")
            .isEqualTo("channel");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testListingReportsAttentionReasonFromTheFacadeDto() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setAutomationWorkflowUuid(WORKFLOW_UUID);

        Workflow workflow = new Workflow(
            WORKFLOW_UUID, JsonUtils.write(Map.of("label", "Sync leads", "description", "d")), Workflow.Format.JSON);

        ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO = ConnectedUserProjectWorkflowDTO.ofReference(
            7L, reference, new WorkflowDTO(workflow, List.of(), List.of()), List.of(), List.of(), Map.of(),
            "MISSING_CONNECTION:slack");

        when(connectedUserProjectFacade.getConnectedUserProjectWorkflows(eq(EXTERNAL_USER_ID), any()))
            .thenReturn(List.of(connectedUserProjectWorkflowDTO));

        webTestClient
            .get()
            .uri("/v1/automation/workflows")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].attentionReason")
            .isEqualTo("MISSING_CONNECTION:slack");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExplicitProvisionForwardsRequestedConnectionsFromTheBody() {
        when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of("slack", 12L)), isNull()))
                .thenReturn(new ConnectedUserProjectWorkflow());

        webTestClient
            .post()
            .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                WORKFLOW_UUID)
            .bodyValue(Map.of("connections", Map.of("slack", 12)))
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(connectedUserWorkflowReferenceFacade).getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of("slack", 12L)), isNull());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExplicitProvisionConnectionWithoutIdReturns400() {
        webTestClient
            .post()
            .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                WORKFLOW_UUID)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"connections\":{\"slack\":null}}")
            .exchange()
            .expectStatus()
            .isBadRequest();

        verify(connectedUserWorkflowReferenceFacade, never())
            .getOrCreateReference(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testExternalUserEnableReferenceMissingInputReturns409() {
        doThrow(new MissingInputException("channel"))
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());

        webTestClient
            .post()
            .uri("/v1/{externalUserId}/automation/workflows/{workflowUuid}/enable", EXTERNAL_USER_ID, WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingInputName")
            .isEqualTo("channel");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testUpdateFrontendProjectWorkflowInputsStoresTheSuppliedValues() {
        webTestClient
            .put()
            .uri("/v1/automation/workflows/{workflowUuid}/inputs", WORKFLOW_UUID)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"inputs\":{\"sheetName\":\"Leads\"}}")
            .exchange()
            .expectStatus()
            .isNoContent();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);

        verify(connectedUserProjectFacade).updateProjectWorkflowInputs(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), captor.capture(), any());

        Map<String, ?> inputs = captor.getValue();

        assertThat(inputs).hasSize(1);
        assertThat((Object) inputs.get("sheetName")).isEqualTo("Leads");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testUpdateFrontendProjectWorkflowInputsMissingInputReturns409() {
        doThrow(new MissingInputException("channel"))
            .when(connectedUserProjectFacade)
            .updateProjectWorkflowInputs(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(), any());

        webTestClient
            .put()
            .uri("/v1/automation/workflows/{workflowUuid}/inputs", WORKFLOW_UUID)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"inputs\":{}}")
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingInputName")
            .isEqualTo("channel");
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testUpdateProjectWorkflowInputsMissingInputReturns409() {
        doThrow(new MissingInputException("channel"))
            .when(connectedUserProjectFacade)
            .updateProjectWorkflowInputs(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(), any());

        webTestClient
            .put()
            .uri("/v1/{externalUserId}/automation/workflows/{workflowUuid}/inputs", EXTERNAL_USER_ID, WORKFLOW_UUID)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"inputs\":{}}")
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.missingInputName")
            .isEqualTo("channel");
    }

    private void expectCopyNotFoundWithoutBody(String workflowUuid) {
        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/copy", workflowUuid)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .isEmpty();
    }

    private void expectFrontendProvisionNotFoundWithoutBody(String workflowUuid) {
        webTestClient
            .post()
            .uri("/v1/automation/workflow-templates/{workflowUuid}/provision", workflowUuid)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .isEmpty();
    }

    private void expectProvisionNotFoundWithoutBody(String workflowUuid) {
        webTestClient
            .post()
            .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                workflowUuid)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .isEmpty();
    }

    @Nested
    @ContextConfiguration(classes = CrossUser.EmbeddedSecurityTestConfiguration.class)
    @TestPropertySource(properties = "openapi.openAPIDefinition.base-path.embedded=/api/embedded")
    class CrossUser {

        private static final String OTHER_EXTERNAL_USER_ID = "someone-else@example.com";
        private static final String PROVISION_PATH =
            "/api/embedded/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision";
        private static final String TENANT_ID = "test_tenant";

        @MockitoBean
        private ApiKeyService apiKeyService;

        @Autowired
        private ConnectedUserService connectedUserService;

        @MockitoBean
        private JwtTokenService jwtTokenService;

        @MockitoBean
        private SigningKeyService signingKeyService;

        private KeyPair keyPair;

        @BeforeEach
        void beforeEach() throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            keyPair = keyPairGenerator.generateKeyPair();

            when(signingKeyService.getPublicKey(anyString(), anyLong())).thenReturn(keyPair.getPublic());

            stubEnabledConnectedUser(EXTERNAL_USER_ID);
            stubEnabledConnectedUser(OTHER_EXTERNAL_USER_ID);
        }

        @Test
        void testExplicitProvisionCrossUserIsForbidden() {
            webTestClient
                .post()
                .uri(PROVISION_PATH, EXTERNAL_USER_ID, WORKFLOW_UUID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createJwt(OTHER_EXTERNAL_USER_ID))
                .exchange()
                .expectStatus()
                .isUnauthorized();

            verify(connectedUserWorkflowReferenceFacade, never())
                .getOrCreateReference(any(), any(), any(), any(), any());
        }

        @Test
        void testDeprovisionCrossUserIsForbidden() {
            webTestClient
                .delete()
                .uri(PROVISION_PATH, EXTERNAL_USER_ID, WORKFLOW_UUID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createJwt(OTHER_EXTERNAL_USER_ID))
                .exchange()
                .expectStatus()
                .isUnauthorized();

            verify(connectedUserWorkflowReferenceFacade, never())
                .deleteReference(any(), any(), any());
        }

        @Test
        void testExplicitProvisionOwnUserReachesTheFacade() {
            when(connectedUserWorkflowReferenceFacade.getOrCreateReference(
                eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()), isNull()))
                    .thenReturn(new ConnectedUserProjectWorkflow());

            webTestClient
                .post()
                .uri(PROVISION_PATH, EXTERNAL_USER_ID, WORKFLOW_UUID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createJwt(EXTERNAL_USER_ID))
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(connectedUserWorkflowReferenceFacade)
                .getOrCreateReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class), eq(Map.of()),
                    isNull());
        }

        @Test
        void testDeprovisionOwnUserReachesTheFacade() {
            webTestClient
                .delete()
                .uri(PROVISION_PATH, EXTERNAL_USER_ID, WORKFLOW_UUID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createJwt(EXTERNAL_USER_ID))
                .exchange()
                .expectStatus()
                .isNoContent();

            verify(connectedUserWorkflowReferenceFacade)
                .deleteReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
        }

        private String createJwt(String subject) {
            String keyId = EncodingUtils.base64EncodeToString(TENANT_ID + ":keyId");

            return Jwts.builder()
                .header()
                .keyId(keyId)
                .and()
                .subject(subject)
                .signWith(keyPair.getPrivate())
                .compact();
        }

        private void stubEnabledConnectedUser(String externalUserId) {
            ConnectedUser connectedUser = mock(ConnectedUser.class);

            when(connectedUser.getExternalId()).thenReturn(externalUserId);
            when(connectedUser.isEnabled()).thenReturn(true);
            when(connectedUserService.fetchConnectedUser(externalUserId, Environment.PRODUCTION.ordinal()))
                .thenReturn(Optional.of(connectedUser));
        }

        @EnableWebSecurity
        static class EmbeddedSecurityTestConfiguration {

            @Bean
            SecurityFilterChain securityFilterChain(
                HttpSecurity httpSecurity, ApiKeyService apiKeyService, ConnectedUserService connectedUserService,
                JwtTokenService jwtTokenService, SigningKeyService signingKeyService) throws Exception {

                httpSecurity
                    .authorizeHttpRequests(authorize -> authorize
                        .anyRequest()
                        .authenticated())
                    .with(
                        new EmbeddedApiKeySecurityConfigurer(
                            apiKeyService, connectedUserService, jwtTokenService, signingKeyService),
                        withDefaults());

                return httpSecurity.build();
            }
        }
    }
}

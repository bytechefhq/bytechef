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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserCodeWorkflowReferenceFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
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
@WebMvcTest(ConnectedUserProjectWorkflowApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
public class ConnectedUserProjectWorkflowApiControllerReferenceIntTest {

    private static final String EXTERNAL_USER_ID = "ext-user-1";
    private static final String WORKFLOW_UUID = "catalog-uuid-1";

    @Autowired
    private ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

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
    public void testExplicitProvisionCreatesAReference() {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        when(connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class)))
                .thenReturn(reference);

        try {
            webTestClient
                .post()
                .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                    WORKFLOW_UUID)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(connectedUserCodeWorkflowReferenceFacade)
            .getOrCreateReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testExplicitProvisionMissingConnectionReturns409() {
        when(connectedUserCodeWorkflowReferenceFacade.getOrCreateReference(
            eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class)))
                .thenThrow(new MissingConnectionException("slack"));

        try {
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
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    /**
     * In the {@code @WebMvcTest} slice, {@link AccessDeniedException} is wrapped in a {@code ServletException} rather
     * than translated to 403 (no full Spring Security {@code ExceptionTranslationFilter}). The important invariant is
     * that the reference facade is NEVER invoked when the cross-user ownership check fails --
     * {@code SecurityUtils.checkCurrentUserLogin} runs before any provisioning work.
     */
    @Test
    @WithMockUser(username = "someone-else@example.com")
    public void testExplicitProvisionCrossUserIsForbidden() {
        boolean exceptionThrown = false;

        try {
            mockMvc.perform(
                post(
                    "/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                    WORKFLOW_UUID));
        } catch (Exception exception) {
            assertThat(exception.getCause()).isInstanceOf(AccessDeniedException.class);
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();

        verify(connectedUserCodeWorkflowReferenceFacade, never())
            .getOrCreateReference(any(), any(), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testDeprovisionDeletesTheReference() {
        try {
            webTestClient
                .delete()
                .uri("/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                    WORKFLOW_UUID)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(connectedUserCodeWorkflowReferenceFacade)
            .deleteReference(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), any(Environment.class));
    }

    @Test
    @WithMockUser(username = "someone-else@example.com")
    public void testDeprovisionCrossUserIsForbidden() {
        boolean exceptionThrown = false;

        try {
            mockMvc.perform(
                delete(
                    "/v1/{externalUserId}/automation/workflow-templates/{workflowUuid}/provision", EXTERNAL_USER_ID,
                    WORKFLOW_UUID));
        } catch (Exception exception) {
            assertThat(exception.getCause()).isInstanceOf(AccessDeniedException.class);
            exceptionThrown = true;
        }

        assertThat(exceptionThrown).isTrue();

        verify(connectedUserCodeWorkflowReferenceFacade, never())
            .deleteReference(any(), any(), any());
    }

    /**
     * The public enable/disable endpoints route to {@link ConnectedUserProjectFacade}'s String-uuid overload of
     * {@code enableProjectWorkflow}, which (after this fix) can resolve {@code workflowUuid} to one of the caller's
     * automation-bridge reference rows and surface the same {@link MissingConnectionException} the explicit provision
     * endpoint above does. This test pins that the enable endpoint maps it to the same 409 shape.
     */
    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testEnableReferenceMissingConnectionReturns409() {
        doThrow(new MissingConnectionException("slack"))
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());

        try {
            webTestClient
                .post()
                .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.missingConnectionComponentName")
                .isEqualTo("slack");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testEnableReferenceSucceeds() {
        doNothing()
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());

        try {
            webTestClient
                .post()
                .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(true), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testDisableReferenceSucceeds() {
        doNothing()
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(false), any());

        try {
            webTestClient
                .delete()
                .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
                .exchange()
                .expectStatus()
                .isNoContent();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(false), any());
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testDisableReferenceMissingConnectionReturns409() {
        doThrow(new MissingConnectionException("slack"))
            .when(connectedUserProjectFacade)
            .enableProjectWorkflow(eq(EXTERNAL_USER_ID), eq(WORKFLOW_UUID), eq(false), any());

        try {
            webTestClient
                .delete()
                .uri("/v1/automation/workflows/{workflowUuid}/enable", WORKFLOW_UUID)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.missingConnectionComponentName")
                .isEqualTo("slack");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }
}

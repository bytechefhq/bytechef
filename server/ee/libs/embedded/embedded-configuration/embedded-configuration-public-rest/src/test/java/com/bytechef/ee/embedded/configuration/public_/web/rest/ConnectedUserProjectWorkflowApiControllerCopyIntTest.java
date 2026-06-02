/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
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
@WebMvcTest(ConnectedUserProjectWorkflowApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
public class ConnectedUserProjectWorkflowApiControllerCopyIntTest {

    private static final String WORKFLOW_UUID = "workflow-uuid-001";

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
    @WithMockUser(username = "user@example.com")
    public void testCopyFrontendWorkflowTemplateReturnsWorkflowUuid() {
        String newWorkflowUuid = "new-workflow-uuid-999";

        when(connectedUserProjectFacade.copyWorkflowTemplate(
            eq("user@example.com"), eq(WORKFLOW_UUID), any(Environment.class)))
                .thenReturn(newWorkflowUuid);

        try {
            webTestClient
                .post()
                .uri("/v1/automation/workflow-templates/{workflowUuid}/copy", WORKFLOW_UUID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo(newWorkflowUuid);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @WithMockUser(username = "user@example.com")
    public void testCopyFrontendWorkflowTemplateUnknownIdReturns404() {
        when(connectedUserProjectFacade.copyWorkflowTemplate(
            eq("user@example.com"), eq("unknown-id"), any(Environment.class)))
                .thenThrow(new IllegalArgumentException("Workflow template not found: unknown-id"));

        try {
            webTestClient
                .post()
                .uri("/v1/automation/workflow-templates/{workflowUuid}/copy", "unknown-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound();
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }
}

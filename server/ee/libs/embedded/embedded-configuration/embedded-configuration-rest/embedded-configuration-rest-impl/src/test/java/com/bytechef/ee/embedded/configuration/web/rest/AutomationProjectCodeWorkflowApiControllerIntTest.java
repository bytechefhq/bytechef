/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.facade.AppEventFacade;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.rest.config.EmbeddedConfigurationRestTestConfiguration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
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
@WebMvcTest(value = AutomationProjectCodeWorkflowApiController.class)
@EmbeddedConfigurationRestConfigurationSharedMocks
class AutomationProjectCodeWorkflowApiControllerIntTest {

    @MockitoBean
    private AppEventFacade appEventFacade;

    @MockitoBean
    private AppEventService appEventService;

    @MockitoBean
    private CategoryService categoryService;

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
    private AutomationWorkflowProjectCodeWorkflowFacade automationWorkflowProjectCodeWorkflowFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();
    }

    @Test
    void testDeployAutomationProjectCodeWorkflowReturnsTheWarnings() {
        byte[] bytes = "export function handler() {}".getBytes(StandardCharsets.UTF_8);

        when(automationWorkflowProjectCodeWorkflowFacade.save(eq(bytes), eq(Language.JAVASCRIPT), any(), anyBoolean()))
            .thenReturn(List.of("Workflow 'charge' has no trigger that can start it"));

        webTestClient
            .post()
            .uri("/internal/automation/projects/deploy")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipartBody("projectFile", "project.js", bytes).build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.warnings[0]")
            .isEqualTo("Workflow 'charge' has no trigger that can start it");

        verify(automationWorkflowProjectCodeWorkflowFacade).save(eq(bytes), eq(Language.JAVASCRIPT), any(),
            anyBoolean());
    }

    @Test
    void testDeployAutomationProjectCodeWorkflowJava() {
        byte[] bytes = {
            0x50, 0x4b, 0x03, 0x04
        };

        when(automationWorkflowProjectCodeWorkflowFacade.save(eq(bytes), eq(Language.JAVA), any(), anyBoolean()))
            .thenReturn(List.of());

        webTestClient
            .post()
            .uri("/internal/automation/projects/deploy")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipartBody("projectFile", "project.jar", bytes).build())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.warnings")
            .isEmpty();

        verify(automationWorkflowProjectCodeWorkflowFacade).save(eq(bytes), eq(Language.JAVA), any(), anyBoolean());
    }

    @Test
    void testDeployAutomationProjectCodeWorkflowPassesTheReleaseFlagAndTheRequestEnvironment() {
        byte[] bytes = "export function handler() {}".getBytes(StandardCharsets.UTF_8);

        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(automationWorkflowProjectCodeWorkflowFacade.save(any(), any(), any(), anyBoolean()))
            .thenReturn(List.of());

        MultipartBodyBuilder multipartBodyBuilder = multipartBody("projectFile", "project.js", bytes);

        multipartBodyBuilder.part("release", "true");

        webTestClient
            .post()
            .uri("/internal/automation/projects/deploy")
            .header("X-Environment", "STAGING")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipartBodyBuilder.build())
            .exchange()
            .expectStatus()
            .isOk();

        verify(automationWorkflowProjectCodeWorkflowFacade)
            .save(eq(bytes), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(true));
    }

    @Test
    void testDeployAutomationProjectCodeWorkflowWithoutTheReleaseFieldPassesFalse() {
        byte[] bytes = "export function handler() {}".getBytes(StandardCharsets.UTF_8);

        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
        when(automationWorkflowProjectCodeWorkflowFacade.save(any(), any(), any(), anyBoolean()))
            .thenReturn(List.of());

        webTestClient
            .post()
            .uri("/internal/automation/projects/deploy")
            .header("X-Environment", "STAGING")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(multipartBody("projectFile", "project.js", bytes).build())
            .exchange()
            .expectStatus()
            .isOk();

        verify(automationWorkflowProjectCodeWorkflowFacade)
            .save(eq(bytes), eq(Language.JAVASCRIPT), eq(Environment.STAGING), eq(false));
    }

    private static MultipartBodyBuilder multipartBody(String partName, String filename, byte[] bytes) {
        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();

        multipartBodyBuilder.part(partName, new ByteArrayResource(bytes) {

            @Override
            public String getFilename() {
                return filename;
            }
        });

        return multipartBodyBuilder;
    }
}

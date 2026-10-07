/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
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
@WebMvcTest(AutomationWorkflowProjectApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
public class AutomationWorkflowProjectApiControllerIntTest {

    private static final String WORKFLOW_UUID = "workflow-uuid-001";
    private static final long PROJECT_ID = 42L;
    private static final String EXTERNAL_USER_ID = "user@example.com";

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
    public void testGetFrontendProjectsReturnsPublishedProjectWithWorkflows() {
        ConnectedUserWorkflowTemplateDTO.Component componentDTO =
            new ConnectedUserWorkflowTemplateDTO.Component("gmail", "Gmail", "gmail-icon-svg");

        ConnectedUserWorkflowTemplateDTO workflowDTO = new ConnectedUserWorkflowTemplateDTO(
            WORKFLOW_UUID, "Welcome Email Workflow", "Sends a welcome email to new users", null,
            List.of(), List.of(componentDTO), List.of(), null);

        AutomationWorkflowProjectDTO projectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID, "Onboarding Project", "New user onboarding automations", null, List.of(), true, 1, 1,
            List.of(workflowDTO), null, true);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(projectDTO));

        try {
            webTestClient
                .get()
                .uri("/v1/automation/projects")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(PROJECT_ID)
                .jsonPath("$[0].name")
                .isEqualTo("Onboarding Project")
                .jsonPath("$[0].description")
                .isEqualTo("New user onboarding automations")
                .jsonPath("$[0].workflowTemplates[0].id")
                .isEqualTo(WORKFLOW_UUID)
                .jsonPath("$[0].workflowTemplates[0].label")
                .isEqualTo("Welcome Email Workflow")
                .jsonPath("$[0].workflowTemplates[0].components[0].name")
                .isEqualTo("gmail")
                .jsonPath("$[0].workflowTemplates[0].components[0].title")
                .isEqualTo("Gmail")
                .jsonPath("$[0].workflowTemplates[0].components[0].icon")
                .isEqualTo("gmail-icon-svg");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testGetFrontendProjectsReturnsEmptyList() {
        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of());

        try {
            webTestClient
                .get()
                .uri("/v1/automation/projects")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$")
                .isArray()
                .jsonPath("$.length()")
                .isEqualTo(0);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testGetFrontendProjectsUnpublishedProjectHasEmptyWorkflows() {
        AutomationWorkflowProjectDTO unpublishedProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID + 1, "Draft Project", "A project with no published version", null, List.of(), false, 1, null,
            List.of(), null, true);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(unpublishedProjectDTO));

        try {
            webTestClient
                .get()
                .uri("/v1/automation/projects")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(PROJECT_ID + 1)
                .jsonPath("$[0].name")
                .isEqualTo("Draft Project")
                .jsonPath("$[0].workflowTemplates")
                .isArray()
                .jsonPath("$[0].workflowTemplates.length()")
                .isEqualTo(0);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testFrontendProjectsOmitHiddenProjectsAndUseThePermissionFilter() {
        AutomationWorkflowProjectDTO visibleProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID, "Visible Project", "Shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            true);

        AutomationWorkflowProjectDTO hiddenProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID + 1, "Hidden Project", "Not shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            false);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(visibleProjectDTO, hiddenProjectDTO));

        try {
            webTestClient
                .get()
                .uri("/v1/automation/projects")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(1)
                .jsonPath("$[0].id")
                .isEqualTo(PROJECT_ID)
                .jsonPath("$[0].name")
                .isEqualTo("Visible Project");
        } catch (Exception exception) {
            Assertions.fail(exception);
        }

        verify(automationWorkflowProjectFacade, never()).getPublishedProjects();
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    public void testApiKeyProjectsKeepHiddenProjects() {
        AutomationWorkflowProjectDTO visibleProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID, "Visible Project", "Shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            true);

        AutomationWorkflowProjectDTO hiddenProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID + 1, "Hidden Project", "Not shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            false);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(visibleProjectDTO, hiddenProjectDTO));

        try {
            webTestClient
                .get()
                .uri("/v1/" + EXTERNAL_USER_ID + "/automation/projects")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.length()")
                .isEqualTo(2)
                .jsonPath("$[0].id")
                .isEqualTo(PROJECT_ID)
                .jsonPath("$[1].id")
                .isEqualTo(PROJECT_ID + 1);
        } catch (Exception exception) {
            Assertions.fail(exception);
        }
    }
}

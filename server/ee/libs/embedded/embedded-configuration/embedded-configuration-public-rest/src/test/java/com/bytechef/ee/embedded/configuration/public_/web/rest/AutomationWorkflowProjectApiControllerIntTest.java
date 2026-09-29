/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.WithMockConnectedUser;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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
class AutomationWorkflowProjectApiControllerIntTest {

    private static final String WORKFLOW_UUID = "workflow-uuid-001";
    private static final String EXTERNAL_USER_ID = "user@example.com";
    private static final long PROJECT_ID = 42L;

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
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testGetFrontendProjectsReturnsPublishedProjectWithWorkflows() {
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
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testGetFrontendProjectsListTheTriggerComponentsOfATemplateFirstAndOnce() {
        ConnectedUserWorkflowTemplateDTO.Component gmailComponent =
            new ConnectedUserWorkflowTemplateDTO.Component("gmail", "Gmail", "gmail-icon-svg", true, 7L);
        ConnectedUserWorkflowTemplateDTO.Component slackComponent =
            new ConnectedUserWorkflowTemplateDTO.Component("slack", "Slack", "slack-icon-svg", true, 8L);

        ConnectedUserWorkflowTemplateDTO workflowDTO = new ConnectedUserWorkflowTemplateDTO(
            WORKFLOW_UUID, "Notify Slack", null, null, List.of(gmailComponent),
            List.of(slackComponent, gmailComponent), List.of(), null);

        AutomationWorkflowProjectDTO projectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID, "Slack Project", null, null, List.of(), true, 1, 1, List.of(workflowDTO), null, true);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(projectDTO));

        webTestClient
            .get()
            .uri("/v1/automation/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].workflowTemplates[0].components.length()")
            .isEqualTo(2)
            .jsonPath("$[0].workflowTemplates[0].components[0].name")
            .isEqualTo("gmail")
            .jsonPath("$[0].workflowTemplates[0].components[0].connectionRequired")
            .isEqualTo(true)
            .jsonPath("$[0].workflowTemplates[0].components[1].name")
            .isEqualTo("slack");
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testGetFrontendProjectsReturnsEmptyList() {
        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of());

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
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testGetFrontendProjectsUnpublishedProjectHasEmptyWorkflows() {
        AutomationWorkflowProjectDTO unpublishedProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID + 1, "Draft Project", "A project with no published version", null, List.of(), false, 1, null,
            List.of(), null, true);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(unpublishedProjectDTO));

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
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testFrontendProjectsKeepHiddenProjectsAndUseThePermissionFilter() {
        AutomationWorkflowProjectDTO visibleProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID, "Visible Project", "Shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            true);

        AutomationWorkflowProjectDTO hiddenProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID + 1, "Hidden Project", "Not shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            false);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(visibleProjectDTO, hiddenProjectDTO));

        webTestClient
            .get()
            .uri("/v1/automation/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.length()")
            .isEqualTo(2)
            .jsonPath("$[0].id")
            .isEqualTo(PROJECT_ID)
            .jsonPath("$[0].automationHubVisible")
            .isEqualTo(true)
            .jsonPath("$[1].id")
            .isEqualTo(PROJECT_ID + 1)
            .jsonPath("$[1].automationHubVisible")
            .isEqualTo(false);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testApiKeyProjectsKeepHiddenProjects() {
        AutomationWorkflowProjectDTO visibleProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID, "Visible Project", "Shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            true);

        AutomationWorkflowProjectDTO hiddenProjectDTO = new AutomationWorkflowProjectDTO(
            PROJECT_ID + 1, "Hidden Project", "Not shown in the hub", null, List.of(), true, 1, 1, List.of(), null,
            false);

        when(automationWorkflowProjectFacade.getPublishedProjects(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(List.of(visibleProjectDTO, hiddenProjectDTO));

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
    }
}

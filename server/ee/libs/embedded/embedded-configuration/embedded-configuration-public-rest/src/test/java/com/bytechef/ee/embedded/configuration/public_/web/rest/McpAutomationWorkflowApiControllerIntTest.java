/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.ai.mcp.exception.McpAutomationWorkflowNotFoundException;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.WithMockConnectedUser;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTriggerDTO;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
@WebMvcTest({
    McpAutomationWorkflowApiController.class, ConnectedUserProjectWorkflowApiController.class
})
@EmbeddedConfigurationPublicRestSharedMocks
class McpAutomationWorkflowApiControllerIntTest {

    private static final long CONNECTED_USER_ID = 7L;
    private static final String EXTERNAL_USER_ID = "external-user";
    private static final String FRONTEND_PATH = "/v1/automation/workflows/{workflowUuid}/mcp/enable";
    private static final String PATH = "/v1/{externalUserId}/automation/workflows/{workflowUuid}/mcp/enable";
    private static final String WORKFLOW_UUID = "automation-uuid";

    @Autowired
    private ConnectedUserProjectFacade connectedUserProjectFacade;

    @Autowired
    private ConnectedUserService connectedUserService;

    @MockitoBean
    private AutomationProjectFacade automationProjectFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private McpAutomationWorkflowFacade mcpAutomationWorkflowFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient
            .bindTo(mockMvc)
            .build();

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(CONNECTED_USER_ID);

        when(environmentService.getEnvironment(any())).thenReturn(Environment.PRODUCTION);
        when(connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUser);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testEnableFrontendUsesTheCurrentConnectedUser() {
        webTestClient
            .post()
            .uri(FRONTEND_PATH, WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(mcpAutomationWorkflowFacade).enableMcpAutomationWorkflow(CONNECTED_USER_ID, WORKFLOW_UUID, true);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testDisableFrontendUsesTheCurrentConnectedUser() {
        webTestClient
            .delete()
            .uri(FRONTEND_PATH, WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(mcpAutomationWorkflowFacade).enableMcpAutomationWorkflow(CONNECTED_USER_ID, WORKFLOW_UUID, false);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testEnableUsesThePathExternalUser() {
        webTestClient
            .post()
            .uri(PATH, EXTERNAL_USER_ID, WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(mcpAutomationWorkflowFacade).enableMcpAutomationWorkflow(CONNECTED_USER_ID, WORKFLOW_UUID, true);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testDisableUsesThePathExternalUser() {
        webTestClient
            .delete()
            .uri(PATH, EXTERNAL_USER_ID, WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNoContent();

        verify(mcpAutomationWorkflowFacade).enableMcpAutomationWorkflow(CONNECTED_USER_ID, WORKFLOW_UUID, false);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testNotFoundWithoutReference() {
        doThrow(new McpAutomationWorkflowNotFoundException(WORKFLOW_UUID))
            .when(mcpAutomationWorkflowFacade)
            .enableMcpAutomationWorkflow(CONNECTED_USER_ID, WORKFLOW_UUID, true);

        webTestClient
            .post()
            .uri(FRONTEND_PATH, WORKFLOW_UUID)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testMcpEnabledNullWhenNotOffered() {
        when(connectedUserProjectFacade.getConnectedUserProjectWorkflows(any(String.class), any()))
            .thenReturn(List.of(referenceDto(List.of())));
        when(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(CONNECTED_USER_ID)).thenReturn(Map.of());

        webTestClient
            .get()
            .uri("/v1/automation/workflows")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].mcpEnabled")
            .doesNotExist();
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testMcpEnabledReportedWhenOffered() {
        when(connectedUserProjectFacade.getConnectedUserProjectWorkflows(any(String.class), any()))
            .thenReturn(List.of(referenceDto(List.of())));
        when(mcpAutomationWorkflowFacade.getMcpEnabledByAutomationUuid(CONNECTED_USER_ID))
            .thenReturn(Map.of(WORKFLOW_UUID, false));

        webTestClient
            .get()
            .uri("/v1/automation/workflows")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].mcpEnabled")
            .isEqualTo(false);
    }

    @Test
    @WithMockConnectedUser(externalUserId = EXTERNAL_USER_ID)
    void testToolOnlyFlag() {
        when(connectedUserProjectFacade.getConnectedUserProjectWorkflows(any(String.class), any()))
            .thenReturn(
                List.of(
                    referenceDto(List.of(trigger("workflow/v1/newWorkflowCall", false))),
                    referenceDto(
                        List.of(trigger("workflow/v1/newWorkflowCall", false), trigger("schedule/v1/cron", false))),
                    referenceDto(
                        List.of(trigger("workflow/v1/newWorkflowCall", false), trigger("schedule/v1/cron", true))),
                    referenceDto(List.of())));

        webTestClient
            .get()
            .uri("/v1/automation/workflows")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$[0].toolOnly")
            .isEqualTo(true)
            .jsonPath("$[1].toolOnly")
            .isEqualTo(false)
            .jsonPath("$[2].toolOnly")
            .isEqualTo(true)
            .jsonPath("$[3].toolOnly")
            .isEqualTo(false);
    }

    private static ConnectedUserProjectWorkflowDTO referenceDto(List<WorkflowTriggerDTO> triggers) {
        ConnectedUserProjectWorkflow reference = new ConnectedUserProjectWorkflow();

        reference.setId(1L);
        reference.setAutomationUuid(WORKFLOW_UUID);

        Workflow workflow = new Workflow(
            WORKFLOW_UUID, JsonUtils.write(Map.of("label", "Send", "description", "d")), Workflow.Format.JSON);

        return ConnectedUserProjectWorkflowDTO.ofReference(
            CONNECTED_USER_ID, reference, new WorkflowDTO(workflow, List.of(), triggers), List.of());
    }

    private static WorkflowTriggerDTO trigger(String type, boolean disabled) {
        return new WorkflowTriggerDTO(List.of(), null, disabled, Map.of(), "trigger_1", null, Map.of(), null, type);
    }
}

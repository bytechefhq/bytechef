/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceToolFacade;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestSharedMocks;
import com.bytechef.ee.embedded.configuration.public_.web.rest.config.EmbeddedConfigurationPublicRestTestConfiguration;
import com.bytechef.platform.configuration.service.EnvironmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
@WebMvcTest(McpIntegrationInstanceToolApiController.class)
@EmbeddedConfigurationPublicRestSharedMocks
class McpIntegrationInstanceToolApiControllerIntTest {

    private static final String API_KEY_PRINCIPAL = "api-key-principal";
    private static final String EXTERNAL_USER_ID = "external-user";
    private static final String FRONTEND_TOOL_ENABLE_PATH =
        "/v1/integration-instances/{id}/mcp-tools/{mcpToolId}/enable";
    private static final long INTEGRATION_INSTANCE_ID = 7L;
    private static final long MCP_TOOL_ID = 3L;
    private static final String TOOL_ENABLE_PATH =
        "/v1/{externalUserId}/integration-instances/{id}/mcp-tools/{mcpToolId}/enable";

    @MockitoBean
    private AutomationWorkflowProjectFacade automationWorkflowProjectFacade;

    @Autowired
    private ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private McpIntegrationInstanceToolFacade mcpIntegrationInstanceToolFacade;

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
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testEnableFrontendMcpIntegrationInstanceToolValidatesOwnershipOfCurrentConnectedUser() {
        webTestClient
            .post()
            .uri(FRONTEND_TOOL_ENABLE_PATH, INTEGRATION_INSTANCE_ID, MCP_TOOL_ID)
            .exchange()
            .expectStatus()
            .isNoContent();

        InOrder inOrder = inOrder(connectedUserIntegrationInstanceFacade, mcpIntegrationInstanceToolFacade);

        inOrder.verify(connectedUserIntegrationInstanceFacade)
            .validateIntegrationInstanceOwnership(EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID);
        inOrder.verify(mcpIntegrationInstanceToolFacade)
            .enableMcpIntegrationInstanceTool(INTEGRATION_INSTANCE_ID, MCP_TOOL_ID, true);
    }

    @Test
    @WithMockUser(username = API_KEY_PRINCIPAL)
    void testDisableMcpIntegrationInstanceToolValidatesOwnershipOfPathExternalUser() {
        webTestClient
            .delete()
            .uri(TOOL_ENABLE_PATH, EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, MCP_TOOL_ID)
            .exchange()
            .expectStatus()
            .isNoContent();

        InOrder inOrder = inOrder(connectedUserIntegrationInstanceFacade, mcpIntegrationInstanceToolFacade);

        inOrder.verify(connectedUserIntegrationInstanceFacade)
            .validateIntegrationInstanceOwnership(EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID);
        inOrder.verify(mcpIntegrationInstanceToolFacade)
            .enableMcpIntegrationInstanceTool(INTEGRATION_INSTANCE_ID, MCP_TOOL_ID, false);
    }

    @Test
    @WithMockUser(username = EXTERNAL_USER_ID)
    void testEnableFrontendMcpIntegrationInstanceToolDeniedForNonOwner() {
        doThrow(new EmbeddedIntegrationNotVisibleException(INTEGRATION_INSTANCE_ID))
            .when(connectedUserIntegrationInstanceFacade)
            .validateIntegrationInstanceOwnership(EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID);

        webTestClient
            .post()
            .uri(FRONTEND_TOOL_ENABLE_PATH, INTEGRATION_INSTANCE_ID, MCP_TOOL_ID)
            .exchange()
            .expectStatus()
            .isNotFound();

        verifyNoInteractions(mcpIntegrationInstanceToolFacade);
    }
}

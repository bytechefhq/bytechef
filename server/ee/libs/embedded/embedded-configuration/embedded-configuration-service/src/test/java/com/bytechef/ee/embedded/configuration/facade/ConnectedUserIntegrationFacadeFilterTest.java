/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Focused tests for the permission visibility decision wired into the connected-user integration facade. Exercises the
 * real {@link EmbeddedPermissionEvaluator} (SpEL) against the facade's integration-level and per-workflow filters. The
 * full list/single end-to-end behavior is covered by the public-rest integration test.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationFacadeFilterTest {

    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator =
        new EmbeddedPermissionEvaluator(SpelEvaluator.create());
    private final IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
    private final ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade = createFacade();

    @Test
    void testNullExpressionIsVisible() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        assertTrue(embeddedPermissionEvaluator.evaluate(null, connectedUser));
    }

    @Test
    void testMetadataMatchIsVisible() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "pro"));

        assertTrue(embeddedPermissionEvaluator.evaluate("metadata['plan'] == 'pro'", connectedUser));
    }

    @Test
    void testIntegrationVisibleWhenNoExpression() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .permissionExpression(null)
            .build();

        assertTrue(connectedUserIntegrationFacade.isIntegrationVisible(integrationDTO, connectedUser));
    }

    @Test
    void testIntegrationHiddenWhenExpressionMismatches() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "free"));

        IntegrationDTO integrationDTO = IntegrationDTO.builder()
            .permissionExpression("metadata['plan'] == 'pro'")
            .build();

        assertFalse(connectedUserIntegrationFacade.isIntegrationVisible(integrationDTO, connectedUser));
    }

    @Test
    void testFilterWorkflowsDropsHiddenWorkflowsAndKeepsOthers() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getMetadata()).thenReturn(Map.of("plan", "free"));

        IntegrationWorkflow gatedWorkflow = mock(IntegrationWorkflow.class);

        when(gatedWorkflow.getUuidAsString()).thenReturn("uuid-gated");
        when(gatedWorkflow.getPermissionExpression()).thenReturn("metadata['plan'] == 'pro'");

        IntegrationWorkflow openWorkflow = mock(IntegrationWorkflow.class);

        when(openWorkflow.getUuidAsString()).thenReturn("uuid-open");
        when(openWorkflow.getPermissionExpression()).thenReturn(null);

        when(integrationWorkflowService.getIntegrationWorkflows(10L))
            .thenReturn(List.of(gatedWorkflow, openWorkflow));

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            IntegrationInstanceConfigurationDTO.builder()
                .integrationId(10L)
                .integrationInstanceConfigurationWorkflows(
                    List.of(workflowDTO(1L, "uuid-gated"), workflowDTO(2L, "uuid-open")))
                .build();

        IntegrationInstanceConfigurationDTO filteredIntegrationInstanceConfigurationDTO =
            connectedUserIntegrationFacade.filterWorkflows(
                integrationInstanceConfigurationDTO, connectedUser, List.of());

        List<IntegrationInstanceConfigurationWorkflowDTO> visibleWorkflows =
            filteredIntegrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows();

        assertEquals(1, visibleWorkflows.size());
        assertEquals("uuid-open", visibleWorkflows.getFirst()
            .workflowUuid());
    }

    @Test
    void testFilterWorkflowsDropsDisabledAndMcpWorkflows() {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        IntegrationWorkflow enabledWorkflow = mock(IntegrationWorkflow.class);

        when(enabledWorkflow.getUuidAsString()).thenReturn("uuid-enabled");
        when(enabledWorkflow.getPermissionExpression()).thenReturn(null);

        IntegrationWorkflow disabledWorkflow = mock(IntegrationWorkflow.class);

        when(disabledWorkflow.getUuidAsString()).thenReturn("uuid-disabled");
        when(disabledWorkflow.getPermissionExpression()).thenReturn(null);

        IntegrationWorkflow mcpWorkflow = mock(IntegrationWorkflow.class);

        when(mcpWorkflow.getUuidAsString()).thenReturn("uuid-mcp");
        when(mcpWorkflow.getPermissionExpression()).thenReturn(null);

        when(integrationWorkflowService.getIntegrationWorkflows(10L))
            .thenReturn(List.of(enabledWorkflow, disabledWorkflow, mcpWorkflow));

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            IntegrationInstanceConfigurationDTO.builder()
                .integrationId(10L)
                .integrationInstanceConfigurationWorkflows(
                    List.of(
                        workflowDTO(1L, "uuid-enabled", true), workflowDTO(2L, "uuid-disabled", false),
                        workflowDTO(3L, "uuid-mcp", true)))
                .build();

        IntegrationInstanceConfigurationDTO filteredIntegrationInstanceConfigurationDTO =
            connectedUserIntegrationFacade.filterWorkflows(
                integrationInstanceConfigurationDTO, connectedUser,
                List.of(new ConnectedUserIntegrationDTO.McpWorkflowInfo(null, null, List.of(), "uuid-mcp")));

        List<IntegrationInstanceConfigurationWorkflowDTO> visibleWorkflows =
            filteredIntegrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows();

        assertEquals(1, visibleWorkflows.size());
        assertEquals("uuid-enabled", visibleWorkflows.getFirst()
            .workflowUuid());
    }

    private static IntegrationInstanceConfigurationWorkflowDTO workflowDTO(long id, String workflowUuid) {
        return workflowDTO(id, workflowUuid, true);
    }

    private static IntegrationInstanceConfigurationWorkflowDTO workflowDTO(
        long id, String workflowUuid, boolean enabled) {

        return new IntegrationInstanceConfigurationWorkflowDTO(
            List.of(), null, null, Map.of(), enabled, id, null, null, null, null, 0, null, "workflowId-" + id,
            workflowUuid);
    }

    private ConnectedUserIntegrationFacadeImpl createFacade() {
        return new ConnectedUserIntegrationFacadeImpl(
            mock(ClusterElementDefinitionService.class), mock(ComponentDefinitionService.class),
            mock(ConnectedUserService.class), mock(ConnectionFacade.class), mock(ConnectionService.class),
            embeddedPermissionEvaluator, mock(IntegrationInstanceConfigurationFacade.class),
            mock(IntegrationInstanceConfigurationService.class),
            mock(IntegrationInstanceConfigurationWorkflowService.class), mock(IntegrationInstanceService.class),
            mock(IntegrationService.class), mock(McpComponentService.class),
            mock(McpIntegrationInstanceConfigurationService.class),
            mock(McpIntegrationInstanceConfigurationWorkflowService.class),
            mock(McpIntegrationInstanceToolService.class), mock(McpServerService.class), mock(McpToolService.class),
            mock(OAuth2ParametersFacade.class), mock(OAuth2Service.class),
            mock(IntegrationInstanceWorkflowService.class), integrationWorkflowService, mock(WorkflowService.class));
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Verifies the connected-user ownership check on {@code deleteIntegrationInstance}. The previous code deleted the
 * instance's workflows unconditionally and gated only the instance delete behind a tautological
 * {@code connectedUser.getExternalId() == externalUserId} test (always true, since the user was fetched by that id).
 * The negative-path {@code never()} assertions lock the fix: nothing is deleted when the instance is not owned by the
 * connected user resolved from {@code externalUserId}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationFacadeDeleteAuthorizationTest {

    private static final String EXTERNAL_USER_ID = "external-user-id";
    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 9L;
    private static final long INTEGRATION_INSTANCE_ID = 7L;
    private static final long OWNING_CONNECTED_USER_ID = 100L;

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);

    private final ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade =
        new ConnectedUserIntegrationFacadeImpl(
            mock(ClusterElementDefinitionService.class), mock(ComponentDefinitionService.class), connectedUserService,
            mock(ConnectionFacade.class), mock(ConnectionService.class), mock(EmbeddedPermissionEvaluator.class),
            mock(IntegrationInstanceConfigurationFacade.class), integrationInstanceConfigurationService,
            mock(IntegrationInstanceConfigurationWorkflowService.class), integrationInstanceService,
            mock(IntegrationService.class), mock(McpComponentService.class),
            mock(McpIntegrationInstanceConfigurationService.class),
            mock(McpIntegrationInstanceConfigurationWorkflowService.class),
            mock(McpIntegrationInstanceToolService.class), mock(McpServerService.class),
            mock(McpToolService.class), mock(OAuth2ParametersFacade.class),
            mock(OAuth2Service.class), integrationInstanceWorkflowService, mock(IntegrationWorkflowService.class),
            mock(WorkflowService.class));

    @Test
    void testDeleteAllowedForOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, OWNING_CONNECTED_USER_ID);

        connectedUserIntegrationFacade.deleteIntegrationInstance(EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID);

        verify(integrationInstanceWorkflowService).deleteByIntegrationInstanceId(INTEGRATION_INSTANCE_ID);
        verify(integrationInstanceService).delete(INTEGRATION_INSTANCE_ID);
    }

    @Test
    void testDeleteDeniedForNonOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, 200L);

        assertThrows(
            AccessDeniedException.class,
            () -> connectedUserIntegrationFacade.deleteIntegrationInstance(EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID));

        verify(integrationInstanceWorkflowService, never()).deleteByIntegrationInstanceId(anyLong());
        verify(integrationInstanceService, never()).delete(anyLong());
    }

    private void setUpInstance(long owningConnectedUserId, long resolvedConnectedUserId) {
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getConnectedUserId()).thenReturn(owningConnectedUserId);
        when(integrationInstance.getIntegrationInstanceConfigurationId())
            .thenReturn(INTEGRATION_INSTANCE_CONFIGURATION_ID);
        when(integrationInstanceService.getIntegrationInstance(INTEGRATION_INSTANCE_ID))
            .thenReturn(integrationInstance);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            mock(IntegrationInstanceConfiguration.class);

        when(integrationInstanceConfiguration.getEnvironment()).thenReturn(Environment.PRODUCTION);
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            INTEGRATION_INSTANCE_CONFIGURATION_ID))
                .thenReturn(integrationInstanceConfiguration);

        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getId()).thenReturn(resolvedConnectedUserId);
        when(connectedUserService.getConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
            .thenReturn(connectedUser);
    }
}

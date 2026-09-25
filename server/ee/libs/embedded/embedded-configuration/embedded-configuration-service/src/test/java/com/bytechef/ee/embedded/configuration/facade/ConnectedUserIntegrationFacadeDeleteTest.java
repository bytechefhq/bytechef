/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationFacadeDeleteTest {

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService =
        mock(IntegrationInstanceWorkflowService.class);
    private final ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade = createFacade();

    @Test
    void testDeleteIntegrationInstanceOfTheConnectedUser() {
        mockIntegrationInstance(7L, 1L);
        mockConnectedUser("external-user-1", 1L);

        connectedUserIntegrationFacade.deleteIntegrationInstance("external-user-1", 7L);

        verify(integrationInstanceWorkflowService).deleteByIntegrationInstanceId(7L);
        verify(integrationInstanceService).delete(7L);
    }

    @Test
    void testDeleteIntegrationInstanceOfAnotherConnectedUserIsRefusedBeforeAnythingIsDeleted() {
        mockIntegrationInstance(7L, 1L);
        mockConnectedUser("external-user-2", 2L);

        assertThatThrownBy(() -> connectedUserIntegrationFacade.deleteIntegrationInstance("external-user-2", 7L))
            .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).deleteByIntegrationInstanceId(anyLong());
        verify(integrationInstanceService, never()).delete(anyLong());
    }

    @Test
    void testDeleteIntegrationInstanceWithoutAConnectedUserIsRefusedBeforeAnythingIsDeleted() {
        mockIntegrationInstance(7L, 1L);

        when(connectedUserService.fetchConnectedUser("external-user-3", Environment.PRODUCTION))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectedUserIntegrationFacade.deleteIntegrationInstance("external-user-3", 7L))
            .isInstanceOf(EmbeddedIntegrationNotVisibleException.class);

        verify(integrationInstanceWorkflowService, never()).deleteByIntegrationInstanceId(anyLong());
        verify(integrationInstanceService, never()).delete(anyLong());
    }

    private void mockConnectedUser(String externalUserId, long connectedUserId) {
        ConnectedUser connectedUser = mock(ConnectedUser.class);

        when(connectedUser.getExternalId()).thenReturn(externalUserId);
        when(connectedUser.getId()).thenReturn(connectedUserId);

        when(connectedUserService.fetchConnectedUser(externalUserId, Environment.PRODUCTION))
            .thenReturn(Optional.of(connectedUser));
    }

    private void mockIntegrationInstance(long integrationInstanceId, long connectedUserId) {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setConnectedUserId(connectedUserId);
        integrationInstance.setIntegrationInstanceConfigurationId(3L);

        when(integrationInstanceService.getIntegrationInstance(integrationInstanceId))
            .thenReturn(integrationInstance);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(3L))
            .thenReturn(integrationInstanceConfiguration);
    }

    private ConnectedUserIntegrationFacadeImpl createFacade() {
        return new ConnectedUserIntegrationFacadeImpl(
            mock(ClusterElementDefinitionService.class), mock(ComponentDefinitionService.class),
            connectedUserService, mock(ConnectionFacade.class), mock(ConnectionService.class),
            mock(EmbeddedPermissionEvaluator.class), mock(IntegrationInstanceConfigurationFacade.class),
            integrationInstanceConfigurationService,
            mock(IntegrationInstanceConfigurationWorkflowService.class), integrationInstanceService,
            mock(IntegrationService.class), mock(McpComponentService.class),
            mock(McpIntegrationInstanceConfigurationService.class),
            mock(McpIntegrationInstanceConfigurationWorkflowService.class),
            mock(McpIntegrationInstanceToolService.class), mock(McpServerService.class), mock(McpToolService.class),
            mock(OAuth2ParametersFacade.class), mock(OAuth2Service.class),
            integrationInstanceWorkflowService, mock(IntegrationWorkflowService.class), mock(WorkflowService.class));
    }
}

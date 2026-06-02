/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Verifies the connected-user ownership checks. The positive/negative asymmetry is the point: because the previous
 * check was tautological (always true), a positive-only test would pass against the buggy code too. The
 * {@code verify(..., never())} assertions on the denial paths lock the fix in place.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserIntegrationInstanceFacadeImplTest {

    private static final String EXTERNAL_USER_ID = "external-user-id";
    private static final long INTEGRATION_INSTANCE_CONFIGURATION_ID = 9L;
    private static final long INTEGRATION_INSTANCE_ID = 7L;
    private static final long OWNING_CONNECTED_USER_ID = 100L;
    private static final String WORKFLOW_ID = "wf-id";
    private static final String WORKFLOW_UUID = "wf-uuid";

    private final ConnectedUserService connectedUserService = mock(ConnectedUserService.class);
    private final EmbeddedWorkflowInputOptionFacade embeddedWorkflowInputOptionFacade =
        mock(EmbeddedWorkflowInputOptionFacade.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceFacade integrationInstanceFacade = mock(IntegrationInstanceFacade.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);

    private final ConnectedUserIntegrationInstanceFacadeImpl connectedUserIntegrationInstanceFacade =
        new ConnectedUserIntegrationInstanceFacadeImpl(
            connectedUserService, embeddedWorkflowInputOptionFacade, integrationInstanceConfigurationService,
            integrationInstanceService, integrationInstanceFacade, integrationWorkflowService);

    @Test
    void testGetWorkflowInputOptionsAllowedForOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, OWNING_CONNECTED_USER_ID);

        Option option = mock(Option.class);

        when(embeddedWorkflowInputOptionFacade.getWorkflowInputOptions(
            anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(List.of(option));

        List<Option> options = connectedUserIntegrationInstanceFacade.getIntegrationInstanceWorkflowInputOptions(
            EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID, "channel", "channelId", Map.of(), null);

        assertEquals(1, options.size());

        verify(embeddedWorkflowInputOptionFacade).getWorkflowInputOptions(
            eq(INTEGRATION_INSTANCE_ID), eq(WORKFLOW_UUID), eq("channel"), eq("channelId"), any(), any());
    }

    @Test
    void testGetWorkflowInputOptionsDeniedForNonOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, 200L);

        List<Option> options = connectedUserIntegrationInstanceFacade.getIntegrationInstanceWorkflowInputOptions(
            EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID, "channel", "channelId", Map.of(), null);

        assertEquals(List.of(), options);

        verifyNoInteractions(embeddedWorkflowInputOptionFacade);
    }

    @Test
    void testGetWorkflowInputOptionsDeniedWhenConnectedUserAbsent() {
        setUpInstance(OWNING_CONNECTED_USER_ID, null);

        List<Option> options = connectedUserIntegrationInstanceFacade.getIntegrationInstanceWorkflowInputOptions(
            EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID, "channel", "channelId", Map.of(), null);

        assertEquals(List.of(), options);

        verifyNoInteractions(embeddedWorkflowInputOptionFacade);
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowAllowedForOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, OWNING_CONNECTED_USER_ID);

        when(integrationWorkflowService.getWorkflowId(INTEGRATION_INSTANCE_ID, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        Map<String, Object> inputs = Map.of("key", "value");

        connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
            EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID, inputs);

        verify(integrationInstanceFacade).updateIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_ID, WORKFLOW_ID, inputs);
    }

    @Test
    void testUpdateIntegrationInstanceWorkflowDeniedForNonOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, 200L);

        assertThrows(
            EmbeddedIntegrationNotVisibleException.class,
            () -> connectedUserIntegrationInstanceFacade.updateIntegrationInstanceWorkflow(
                EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID, Map.of("key", "value")));

        verify(integrationInstanceFacade, never()).updateIntegrationInstanceWorkflow(anyLong(), anyString(), any());
    }

    @Test
    void testEnableIntegrationInstanceWorkflowAllowedForOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, OWNING_CONNECTED_USER_ID);

        when(integrationWorkflowService.getWorkflowId(INTEGRATION_INSTANCE_ID, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
            EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID);

        verify(integrationInstanceFacade).enableIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_ID, WORKFLOW_ID, true);
    }

    @Test
    void testEnableIntegrationInstanceWorkflowDeniedForNonOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, 200L);

        assertThrows(
            EmbeddedIntegrationNotVisibleException.class,
            () -> connectedUserIntegrationInstanceFacade.enableIntegrationInstanceWorkflow(
                EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID));

        verify(integrationInstanceFacade, never()).enableIntegrationInstanceWorkflow(anyLong(), anyString(), eq(true));
    }

    @Test
    void testDisableIntegrationInstanceWorkflowAllowedForOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, OWNING_CONNECTED_USER_ID);

        when(integrationWorkflowService.getWorkflowId(INTEGRATION_INSTANCE_ID, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
            EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID);

        verify(integrationInstanceFacade).enableIntegrationInstanceWorkflow(
            INTEGRATION_INSTANCE_ID, WORKFLOW_ID, false);
    }

    @Test
    void testDisableIntegrationInstanceWorkflowDeniedForNonOwner() {
        setUpInstance(OWNING_CONNECTED_USER_ID, 200L);

        assertThrows(
            EmbeddedIntegrationNotVisibleException.class,
            () -> connectedUserIntegrationInstanceFacade.disableIntegrationInstanceWorkflow(
                EXTERNAL_USER_ID, INTEGRATION_INSTANCE_ID, WORKFLOW_UUID));

        verify(integrationInstanceFacade, never()).enableIntegrationInstanceWorkflow(anyLong(), anyString(), eq(false));
    }

    private void setUpInstance(Long owningConnectedUserId, Long resolvedConnectedUserId) {
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

        if (resolvedConnectedUserId == null) {
            when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
                .thenReturn(Optional.empty());
        } else {
            ConnectedUser connectedUser = mock(ConnectedUser.class);

            when(connectedUser.getId()).thenReturn(resolvedConnectedUserId);
            when(connectedUserService.fetchConnectedUser(EXTERNAL_USER_ID, Environment.PRODUCTION))
                .thenReturn(Optional.of(connectedUser));
        }
    }
}

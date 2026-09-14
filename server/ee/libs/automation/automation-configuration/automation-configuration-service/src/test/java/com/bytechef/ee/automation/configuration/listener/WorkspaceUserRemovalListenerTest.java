/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.ee.automation.configuration.facade.ConnectionReassignmentFacade;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUserRemovalListenerTest {

    @Mock
    private ConnectionReassignmentFacade connectionReassignmentFacade;

    @Mock
    private ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Mock
    private ResourceGrantService resourceGrantService;

    @Mock
    private WorkspaceConnectionService workspaceConnectionService;

    @InjectMocks
    private WorkspaceUserRemovalListener listener;

    @Test
    void testOnWorkspaceUserRemovedDelegatesToFacade() {
        WorkspaceUserRemovedEvent event = new WorkspaceUserRemovedEvent(42L, 7L, "removed@example.com");

        when(connectionReassignmentFacade.markConnectionsPendingReassignment(42L, "removed@example.com"))
            .thenReturn(new BulkReassignResultDTO(0, 0, 0, 0, List.of()));

        listener.onWorkspaceUserRemoved(event);

        verify(connectionReassignmentFacade).markConnectionsPendingReassignment(42L, "removed@example.com");
    }

    @Test
    void testOnWorkspaceUserRemovedSwallowsFacadeExceptionSoOuterTransactionStaysCommitted() {
        WorkspaceUserRemovedEvent event = new WorkspaceUserRemovedEvent(42L, 7L, "removed@example.com");

        doThrow(new RuntimeException("transient DB failure"))
            .when(connectionReassignmentFacade)
            .markConnectionsPendingReassignment(42L, "removed@example.com");

        assertThatCode(() -> listener.onWorkspaceUserRemoved(event)).doesNotThrowAnyException();

        verify(connectionReassignmentFacade).markConnectionsPendingReassignment(42L, "removed@example.com");
    }

    @Test
    void testRevokeConnectionGrantsRemovesTheRemovedUsersGrantsOnWorkspaceConnections() {
        when(workspaceConnectionService.getWorkspaceConnections(42L))
            .thenReturn(List.of(new WorkspaceConnection(100L, 42L), new WorkspaceConnection(101L, 42L)));

        listener.revokeConnectionGrants(new WorkspaceUserRemovedEvent(42L, 7L, "removed@example.com"));

        verify(resourceGrantService).revokeUserGrants("Connection", 7L, List.of(100L, 101L));
    }
}

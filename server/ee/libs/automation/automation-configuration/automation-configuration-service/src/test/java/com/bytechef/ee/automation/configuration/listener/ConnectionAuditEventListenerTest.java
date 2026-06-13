/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.listener;

import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent;
import com.bytechef.ee.platform.connection.audit.ConnectionAuditPublisher;
import com.bytechef.platform.connection.event.ConnectionCreatedEvent;
import com.bytechef.platform.connection.event.ConnectionDeletedEvent;
import com.bytechef.platform.connection.event.ConnectionWorkflowPausedEvent;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ConnectionAuditEventListenerTest {

    @Mock
    private ConnectionAuditPublisher connectionAuditPublisher;

    @InjectMocks
    private ConnectionAuditEventListener listener;

    @Test
    void testOnConnectionCreatedPublishesAuditWithPersistedVisibility() {
        listener.onConnectionCreated(new ConnectionCreatedEvent(42L, ResourceVisibility.WORKSPACE));

        verify(connectionAuditPublisher).publish(
            ConnectionAuditEvent.CONNECTION_CREATED, 42L, Map.of("visibility", "WORKSPACE"));
    }

    @Test
    void testOnConnectionDeletedPublishesAuditWithNoAdditionalData() {
        listener.onConnectionDeleted(new ConnectionDeletedEvent(7L));

        verify(connectionAuditPublisher).publish(ConnectionAuditEvent.CONNECTION_DELETED, 7L, Map.of());
    }

    @Test
    void testOnConnectionWorkflowPausedPublishesAuditWithEventData() {
        Map<String, Object> data = Map.of(
            "projectDeploymentId", 1L,
            "workflowId", "wf-1",
            "connectionStatus", "PENDING_REASSIGNMENT");

        listener.onConnectionWorkflowPaused(new ConnectionWorkflowPausedEvent(100L, data));

        verify(connectionAuditPublisher).publish(ConnectionAuditEvent.WORKFLOW_PAUSED, 100L, data);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.listener;

import com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent;
import com.bytechef.ee.platform.connection.audit.ConnectionAuditPublisher;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.event.ConnectionCreatedEvent;
import com.bytechef.platform.connection.event.ConnectionDeletedEvent;
import com.bytechef.platform.connection.event.ConnectionWorkflowPausedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Translates the plain CE connection-lifecycle domain events into audit records via the (still-CE)
 * {@link ConnectionAuditPublisher}. This is the EE-only audit seam for CE-origin create/delete/workflow-paused signals.
 *
 * <p>
 * The create/delete handlers run {@code AFTER_COMMIT}: the audit fires only once the surrounding facade transaction has
 * durably committed, so a rolled-back create/delete never emits a success event. As an accepted tradeoff versus the
 * previous strict {@code @AuditConnection} aspect, an audit failure here no longer rolls back the originating
 * connection operation (the operation is already committed by the time this listener runs).
 *
 * <p>
 * The workflow-paused handler instead uses a plain synchronous {@link EventListener}: it is published from
 * job-principal resolution / workflow-execution paths that are NOT inside a Spring-managed transaction (the publishing
 * facade method is {@code @Transactional(propagation = NEVER)} and the {@code JobPrincipalAccessor} is a plain bean).
 * With no active transaction, an {@code AFTER_COMMIT} {@code @TransactionalEventListener} would be silently dropped (no
 * commit phase), losing the audit entirely. A plain {@code @EventListener} guarantees the audit always records.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectionAuditEventListener {

    private final ConnectionAuditPublisher connectionAuditPublisher;

    public ConnectionAuditEventListener(ConnectionAuditPublisher connectionAuditPublisher) {
        this.connectionAuditPublisher = connectionAuditPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConnectionCreated(ConnectionCreatedEvent event) {
        connectionAuditPublisher.publish(
            ConnectionAuditEvent.CONNECTION_CREATED, event.connectionId(),
            Map.of("visibility", event.visibility()
                .name()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConnectionDeleted(ConnectionDeletedEvent event) {
        connectionAuditPublisher.publish(ConnectionAuditEvent.CONNECTION_DELETED, event.connectionId(), Map.of());
    }

    // Plain @EventListener (not @TransactionalEventListener): the workflow-paused signal originates outside any
    // Spring-managed transaction, so an AFTER_COMMIT listener would silently drop the audit. See class Javadoc.
    @EventListener
    public void onConnectionWorkflowPaused(ConnectionWorkflowPausedEvent event) {
        connectionAuditPublisher.publish(ConnectionAuditEvent.WORKFLOW_PAUSED, event.connectionId(), event.data());
    }
}

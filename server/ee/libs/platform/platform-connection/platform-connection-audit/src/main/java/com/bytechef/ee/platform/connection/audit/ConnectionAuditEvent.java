/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.audit;

/**
 * Audit event types emitted through {@link ConnectionAuditPublisher}.
 *
 * <p>
 * Naming convention: {@code CONNECTION_*} events describe state transitions on the connection aggregate.
 * {@code WORKFLOW_PAUSED} is emitted from {@code ProjectDeploymentJobPrincipalAccessor} when a connection's state
 * forces a workflow to pause — the audit subject is still the connection, so it lives here rather than in a separate
 * workflow-audit enum. Future workflow-centric events should go in their own enum to keep this one focused on
 * connection-lifecycle semantics.
 *
 * <p>
 * The payload key contract below documents the fields each event carries beyond the implicit {@code connectionId}
 * (which {@link ConnectionAuditPublisher} attaches to every event). Call sites emit via {@link AuditConnection}
 * annotations or {@link ConnectionAuditPublisher#publish} directly; this contract is convention-enforced rather than
 * type-checked, so changes must be applied at every emitter.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ConnectionAuditEvent {

    /**
     * A new connection was persisted. Payload: {@code visibility} (final persisted {@code ResourceVisibility.name()},
     * reflecting any CE/Embedded force-PRIVATE downgrade).
     */
    CONNECTION_CREATED(false),

    /**
     * A connection was deleted. Payload: no additional keys required; {@code connectionId} identifies the now-removed
     * row. Emitted from the facade delete methods through the {@link AuditConnection} aspect, which defers publishing
     * to {@code afterCommit} so a rolled-back delete does not emit a success event.
     *
     * <p>
     * Marked {@code strictAudit} — a deletion without a trail is a compliance blind spot, so an SpEL-evaluation failure
     * during audit capture rolls back the delete itself rather than letting the row disappear with only a metric.
     */
    CONNECTION_DELETED(true),

    /**
     * A connection's reach was changed. Payload: {@code toVisibility} ({@code ResourceVisibility.name()}).
     *
     * <p>
     * Replaces the separate CONNECTION_PROMOTED / CONNECTION_DEMOTED pair, which encoded a one-way promote-from-private
     * model that no longer exists — visibility now moves freely between the rungs a resource type supports, so "which
     * direction" is a property of the payload rather than of the event type. Historical rows carrying the old names
     * remain readable; the enum is consumed by name, never by ordinal.
     *
     * <p>
     * Marked {@code strictAudit} — this event can narrow reach, and a missing trail would obscure who lost access to a
     * shared credential.
     */
    CONNECTION_VISIBILITY_CHANGED(true),

    /**
     * A named user was granted access to a withheld connection. Payload: {@code targetUserId}.
     */
    CONNECTION_ACCESS_GRANTED(false),

    /**
     * A named user's access to a withheld connection was revoked. Payload: {@code targetUserId}.
     *
     * <p>
     * Marked {@code strictAudit} for the same reason as a visibility narrowing: it removes someone's access, and that
     * must not be able to happen untraceably.
     */
    CONNECTION_ACCESS_REVOKED(true),

    /**
     * Connection ownership transferred to a new user. Payload: {@code newOwnerLogin}.
     *
     * <p>
     * Marked {@code strictAudit} — ownership transfer is the prototypical compliance event: a credential changes hands,
     * and the "who, when, to whom" trail must exist or the transfer is rolled back.
     */
    CONNECTION_REASSIGNED(true),

    /**
     * A workflow was paused because the connection it depends on is no longer usable. Payload: {@code workflowId} (and
     * any additional context the emitting {@code JobPrincipalAccessor} attaches).
     */
    WORKFLOW_PAUSED(false);

    private final boolean strictAudit;

    ConnectionAuditEvent(boolean strictAudit) {
        this.strictAudit = strictAudit;
    }

    /**
     * When {@code true}, an audit-capture failure (SpEL evaluation error in the {@code @AuditConnection} aspect) must
     * fail the business transaction rather than be absorbed into the {@code bytechef_connection_audit_failed} counter.
     * Reserved for privilege-narrowing and ownership-transfer events where a missing trail is a compliance-grade
     * regression. The afterCommit publish step is still best-effort for every event — this flag governs the pre-commit
     * evaluation step only.
     */
    public boolean isStrictAudit() {
        return strictAudit;
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.audit;

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
 * @author Ivica Cardic
 */
public enum ConnectionAuditEvent {

    /**
     * A new connection was persisted. Payload: {@code visibility} (final persisted {@code ConnectionVisibility.name()},
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
     * A connection's visibility was narrowed. Payload: {@code toVisibility} ({@code ConnectionVisibility.name()}).
     * Typically paired with a {@link #CONNECTION_SHARES_REPLACED} umbrella when a share list is emptied.
     *
     * <p>
     * Marked {@code strictAudit} — demotion is a privilege-narrowing event whose absence from the audit trail would
     * obscure who lost access to a shared credential.
     */
    CONNECTION_DEMOTED(true),

    /**
     * A connection's visibility was widened. Payload: {@code fromVisibility}, {@code toVisibility} (both
     * {@code ConnectionVisibility.name()}).
     */
    CONNECTION_PROMOTED(false),

    /**
     * Connection ownership transferred to a new user. Payload: {@code newOwnerLogin}.
     *
     * <p>
     * Marked {@code strictAudit} — ownership transfer is the prototypical compliance event: a credential changes hands,
     * and the "who, when, to whom" trail must exist or the transfer is rolled back.
     */
    CONNECTION_REASSIGNED(true),

    /**
     * A project-level share was revoked from a connection. Payload: {@code projectId} (stringified). When emitted under
     * a {@link #CONNECTION_SHARES_REPLACED} umbrella the event also carries the inherited {@code correlationId}.
     *
     * <p>
     * Marked {@code strictAudit} — revocation is privilege-narrowing, same reasoning as {@link #CONNECTION_DEMOTED}.
     */
    CONNECTION_REVOKED(true),

    /**
     * A connection was granted access to a specific project. Payload: {@code projectId} (stringified). When emitted
     * under a {@link #CONNECTION_SHARES_REPLACED} umbrella the event also carries the inherited {@code correlationId}.
     */
    CONNECTION_SHARED(false),

    /**
     * Bulk replace of a connection's project share list. Emitted once by {@code setConnectionProjects} as the umbrella
     * event; per-row {@link #CONNECTION_SHARED} and {@link #CONNECTION_REVOKED} events inherit the same
     * {@code correlationId} so auditors can reassemble the diff. Payload: {@code requestedProjectIds} (string rendering
     * of the requested list), {@code correlationId}.
     */
    CONNECTION_SHARES_REPLACED(false),

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

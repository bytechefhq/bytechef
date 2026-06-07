/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.audit;

/**
 * Audit event types emitted through {@link ContextStoreSourceAuditPublisher} for Context Store source lifecycle
 * mutations.
 *
 * <p>
 * Naming convention: {@code CONTEXT_STORE_SOURCE_*} events describe state transitions on the Context Store source
 * aggregate. Every event implicitly carries a {@code sourceId} attached by {@link ContextStoreSourceAuditPublisher}.
 * Periodic operations such as refresh/sync are intentionally NOT audited here: they can fire on every cron tick and
 * would flood the audit log.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum ContextStoreSourceAuditEvent {

    /**
     * A new Context Store source was persisted. Payload: {@code workspaceId} and, when available, {@code name}.
     */
    CONTEXT_STORE_SOURCE_CREATED,

    /**
     * A Context Store source was deleted. Payload: {@code workspaceId}; {@code sourceId} identifies the now-removed
     * row.
     */
    CONTEXT_STORE_SOURCE_DELETED,

    /**
     * A Context Store source was enabled. Payload: {@code workspaceId}.
     */
    CONTEXT_STORE_SOURCE_ENABLED,

    /**
     * A Context Store source was disabled. Payload: {@code workspaceId}.
     */
    CONTEXT_STORE_SOURCE_DISABLED
}

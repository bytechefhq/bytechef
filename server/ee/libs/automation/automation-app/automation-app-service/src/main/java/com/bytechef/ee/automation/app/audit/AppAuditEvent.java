/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.audit;

/**
 * Audit event types emitted through {@link AppAuditPublisher}.
 *
 * <p>
 * {@code APP_*} events describe state transitions on the app aggregate. Every event carries an implicit {@code appId}
 * that {@link AppAuditPublisher} attaches automatically.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AppAuditEvent {

    /**
     * A new app was persisted. Payload: {@code name}.
     */
    APP_CREATED,

    /**
     * An app was deleted. Payload: no additional keys required; {@code appId} identifies the now-removed row.
     */
    APP_DELETED,

    /**
     * An app was enabled. Payload: {@code appId} identifies the app.
     */
    APP_ENABLED,

    /**
     * An app was disabled. Payload: {@code appId} identifies the app.
     */
    APP_DISABLED,

    /**
     * An app secret key was rotated. Payload: {@code appId} identifies the app.
     */
    APP_SECRET_KEY_ROTATED,

    /**
     * An app connection was bound or rebound to another connection. Payload: {@code appId}, {@code bindingKey},
     * {@code connectionId}; {@code previousConnectionId} on a rebind.
     */
    APP_CONNECTION_BOUND,

    /**
     * An app connection was unbound. Payload: {@code appId}, {@code bindingKey}.
     */
    APP_CONNECTION_UNBOUND,

    /**
     * The operations of an existing binding were changed without changing its connection. Payload: {@code appId},
     * {@code bindingKey}, {@code connectionId}.
     */
    APP_OPERATIONS_CHANGED,

    /**
     * A workflow was bound to an app. Payload: {@code workflowKey}, {@code projectDeploymentId}, {@code workflowUuid};
     * {@code previousProjectDeploymentId} and {@code previousWorkflowUuid} when the key pointed at another workflow
     * before.
     */
    APP_WORKFLOW_BOUND,

    /**
     * A workflow binding was removed. Payload: {@code workflowKey}.
     */
    APP_WORKFLOW_UNBOUND,

    /**
     * An API collection was bound to an app. Payload: {@code apiCollectionId}, {@code name}.
     */
    APP_API_COLLECTION_BOUND,

    /**
     * An API collection binding was removed. Payload: {@code apiCollectionId}.
     */
    APP_API_COLLECTION_UNBOUND,

    /**
     * An app's editable fields were changed. Payload: {@code changedFields}, the names of the fields whose value
     * changed among {@code name}, {@code description}, {@code hostingTarget}, {@code url}, {@code lastDeployedDate} and
     * {@code visibility}; plus the new {@code name}, {@code visibility} and {@code hostingTarget} when that field
     * changed. Published only when at least one field changed.
     */
    APP_UPDATED
}

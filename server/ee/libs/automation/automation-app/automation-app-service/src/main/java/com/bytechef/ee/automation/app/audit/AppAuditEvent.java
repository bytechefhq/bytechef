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
     * A new app was persisted. Payload: {@code name}, {@code workspaceId}, {@code portalDiscoverable}; plus
     * {@code categoryId} when a category is attached.
     */
    APP_CREATED,

    /**
     * An app was deleted. Payload: no additional keys required; {@code appId} identifies the now-removed row.
     */
    APP_DELETED,

    /**
     * An app was activated. Payload: {@code appId} identifies the app.
     */
    APP_ACTIVATED,

    /**
     * An app was suspended; none of its instances accepts its key until it is activated again. Payload: {@code appId}
     * identifies the app.
     */
    APP_SUSPENDED,

    /**
     * An app was made discoverable in the portal, on creation or later. Payload: {@code appId} identifies the app.
     */
    APP_PUBLISHED_TO_PORTAL,

    /**
     * An app stopped being discoverable in the portal. Payload: {@code appId} identifies the app.
     */
    APP_UNPUBLISHED_FROM_PORTAL,

    /**
     * An app was archived. Payload: {@code appId} identifies the app.
     */
    APP_ARCHIVED,

    /**
     * An instance of an app was created. Payload: {@code appInstanceId}, {@code name}, {@code workspaceId},
     * {@code environment}.
     */
    APP_INSTANCE_CREATED,

    /**
     * An instance's editable fields were changed. Payload: {@code appInstanceId}, {@code changedFields} among
     * {@code name}, {@code hostingTarget}, {@code url} and {@code visibility}; plus the new {@code name},
     * {@code hostingTarget} and {@code visibility} when that field changed. Published only when a field changed.
     */
    APP_INSTANCE_UPDATED,

    /**
     * An instance was activated. Payload: {@code appInstanceId}.
     */
    APP_INSTANCE_ACTIVATED,

    /**
     * An instance was suspended. Payload: {@code appInstanceId}.
     */
    APP_INSTANCE_SUSPENDED,

    /**
     * An instance was terminated: its bindings were removed and its row kept. Payload: {@code appInstanceId}.
     */
    APP_INSTANCE_TERMINATED,

    /**
     * An instance was deleted. Payload: {@code appInstanceId}.
     */
    APP_INSTANCE_DELETED,

    /**
     * An instance's secret key was rotated. Payload: {@code appInstanceId}.
     */
    APP_SECRET_KEY_ROTATED,

    /**
     * An instance's sign-in mode was changed. Payload: {@code appInstanceId}, {@code userAuthMode},
     * {@code previousUserAuthMode}.
     */
    APP_USER_AUTH_CHANGED,

    /**
     * A deployment of an instance reached {@code READY}, including one recorded after someone else released the code.
     * Payload: {@code appInstanceId}, {@code appDeploymentId}, {@code appVersionId}, {@code versionName},
     * {@code provider}.
     */
    APP_DEPLOYMENT_COMPLETED,

    /**
     * A deployment of an instance was queued and handed to its provider. Payload: {@code appInstanceId},
     * {@code appDeploymentId}, {@code appVersionId}, {@code versionName}, {@code provider}; {@code rollback} when it
     * deploys again a version that was once ready.
     */
    APP_DEPLOYMENT_INITIATED,

    /**
     * A provider deployment of an instance failed, including one that did not finish in time. Payload:
     * {@code appInstanceId}, {@code appDeploymentId}, {@code appVersionId}, {@code provider}.
     */
    APP_DEPLOYMENT_FAILED,

    /**
     * A provider deployment of an instance was canceled. Payload: {@code appInstanceId}, {@code appDeploymentId},
     * {@code appVersionId}, {@code provider}.
     */
    APP_DEPLOYMENT_CANCELED,

    /**
     * An app connection was bound or rebound to another connection. Payload: {@code appInstanceId}, {@code bindingKey},
     * {@code connectionId}; {@code previousConnectionId} on a rebind.
     */
    APP_CONNECTION_BOUND,

    /**
     * An app connection was unbound. Payload: {@code appInstanceId}, {@code bindingKey}.
     */
    APP_CONNECTION_UNBOUND,

    /**
     * The operations of an existing binding were changed without changing its connection. Payload:
     * {@code appInstanceId}, {@code bindingKey}, {@code connectionId}.
     */
    APP_OPERATIONS_CHANGED,

    /**
     * A workflow was bound to an app instance. Payload: {@code appInstanceId}, {@code workflowKey},
     * {@code projectDeploymentId}, {@code workflowUuid}; {@code previousProjectDeploymentId} and
     * {@code previousWorkflowUuid} when the key pointed at another workflow before.
     */
    APP_WORKFLOW_BOUND,

    /**
     * A workflow binding was removed. Payload: {@code appInstanceId}, {@code workflowKey}.
     */
    APP_WORKFLOW_UNBOUND,

    /**
     * An API collection was bound to an app instance. Payload: {@code appInstanceId}, {@code apiCollectionId},
     * {@code name}.
     */
    APP_API_COLLECTION_BOUND,

    /**
     * An API collection binding was removed. Payload: {@code appInstanceId}, {@code apiCollectionId}.
     */
    APP_API_COLLECTION_UNBOUND,

    /**
     * An app's editable fields were changed. Payload: {@code changedFields}, the names of the fields whose value
     * changed among {@code name}, {@code description}, {@code icon}, {@code categoryId} and {@code portalDiscoverable};
     * plus the new {@code name}, {@code portalDiscoverable} and {@code categoryId} (absent when the category was
     * cleared) for the fields that changed. Published only when at least one field changed.
     */
    APP_UPDATED,

    /**
     * An app's visibility was set. Payload: {@code visibility}, {@code previousVisibility}.
     */
    APP_VISIBILITY_CHANGED,

    /**
     * A workspace member was granted access to an app. Payload: {@code userId}.
     */
    APP_ACCESS_GRANTED,

    /**
     * A grant on an app was revoked. Payload: {@code userId}.
     */
    APP_ACCESS_REVOKED,

    /**
     * An instance's visibility was set. Payload: {@code appInstanceId}, {@code visibility}, {@code previousVisibility}.
     */
    APP_INSTANCE_VISIBILITY_CHANGED,

    /**
     * A member of the instance's workspace was granted access to an instance. Payload: {@code appInstanceId},
     * {@code userId}.
     */
    APP_INSTANCE_ACCESS_GRANTED,

    /**
     * A grant on an instance was revoked. Payload: {@code appInstanceId}, {@code userId}.
     */
    APP_INSTANCE_ACCESS_REVOKED,

    /**
     * The owner of a PRIVATE app left the app's workspace, so only a tenant admin can manage it now. Payload:
     * {@code userId}, {@code workspaceId}.
     */
    APP_OWNER_LEFT,

    /**
     * The owner of a PRIVATE instance left the instance's workspace, so only a tenant admin can manage it now. Payload:
     * {@code appInstanceId}, {@code userId}, {@code workspaceId}.
     */
    APP_INSTANCE_OWNER_LEFT
}

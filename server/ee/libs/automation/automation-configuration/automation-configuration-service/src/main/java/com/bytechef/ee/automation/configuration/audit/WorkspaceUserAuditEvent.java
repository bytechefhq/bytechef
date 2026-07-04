/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * Audit event types emitted through {@link WorkspaceUserAuditPublisher} for membership mutations on a workspace. Every
 * event carries {@code workspaceId} and {@code userId} in its payload; {@code role}-bearing events additionally include
 * {@code role}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum WorkspaceUserAuditEvent {

    /**
     * A user was added as a member of a workspace. Payload: {@code workspaceId}, {@code userId}, {@code role}.
     */
    WORKSPACE_USER_ADDED,

    /**
     * A user was removed from a workspace. Payload: {@code workspaceId}, {@code userId}.
     */
    WORKSPACE_USER_REMOVED,

    /**
     * An existing member's workspace role was updated. Payload: {@code workspaceId}, {@code userId}, {@code role}.
     */
    WORKSPACE_USER_ROLE_UPDATED
}

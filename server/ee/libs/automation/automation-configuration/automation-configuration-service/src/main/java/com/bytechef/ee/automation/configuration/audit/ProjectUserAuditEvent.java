/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * Audit event types emitted through {@link ProjectUserAuditPublisher} for membership mutations on a project. Every
 * event carries {@code projectId} and {@code userId} in its payload; {@code role}-bearing events additionally include
 * {@code role}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ProjectUserAuditEvent {

    /**
     * A user was added as a member of a project. Payload: {@code projectId}, {@code userId}, {@code role}.
     */
    PROJECT_USER_ADDED,

    /**
     * A user was removed from a project. Payload: {@code projectId}, {@code userId}.
     */
    PROJECT_USER_REMOVED,

    /**
     * An existing member's project role was updated. Payload: {@code projectId}, {@code userId}, {@code role}.
     */
    PROJECT_USER_ROLE_UPDATED
}

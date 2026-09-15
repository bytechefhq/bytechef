/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * Audit event names for workspace membership changes. Strings, not an enum, because {@code @Audited#event()} needs a
 * compile-time constant; the values match what earlier releases stored.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkspaceUserAuditEvents {

    public static final String WORKSPACE_USER_ADDED = "WORKSPACE_USER_ADDED";
    public static final String WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED = "WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED";
    public static final String WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED = "WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED";
    public static final String WORKSPACE_USER_REMOVED = "WORKSPACE_USER_REMOVED";
    public static final String WORKSPACE_USER_ROLE_UPDATED = "WORKSPACE_USER_ROLE_UPDATED";

    private WorkspaceUserAuditEvents() {
    }
}

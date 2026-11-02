/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class WorkspaceAuditEvents {

    public static final String WORKSPACE_CREATED = "WORKSPACE_CREATED";
    public static final String WORKSPACE_DELETED = "WORKSPACE_DELETED";
    public static final String WORKSPACE_UPDATED = "WORKSPACE_UPDATED";

    private WorkspaceAuditEvents() {
    }
}

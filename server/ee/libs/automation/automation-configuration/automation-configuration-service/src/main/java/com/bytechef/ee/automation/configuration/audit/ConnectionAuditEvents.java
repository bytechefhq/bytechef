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
public final class ConnectionAuditEvents {

    public static final String CONNECTION_ACCESS_GRANTED = "CONNECTION_ACCESS_GRANTED";
    public static final String CONNECTION_ACCESS_REVOKED = "CONNECTION_ACCESS_REVOKED";
    public static final String CONNECTION_CREATED = "CONNECTION_CREATED";
    public static final String CONNECTION_CREDENTIALS_UPDATED = "CONNECTION_CREDENTIALS_UPDATED";
    public static final String CONNECTION_DELETED = "CONNECTION_DELETED";
    public static final String CONNECTION_REASSIGNED = "CONNECTION_REASSIGNED";
    public static final String CONNECTION_VISIBILITY_CHANGED = "CONNECTION_VISIBILITY_CHANGED";

    private ConnectionAuditEvents() {
    }
}

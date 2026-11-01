/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ConnectionAuditEvents {

    public static final String CONNECTION_CREATED = "CONNECTION_CREATED";
    public static final String CONNECTION_DELETED = "CONNECTION_DELETED";
    public static final String CONNECTION_REAUTHORIZED = "CONNECTION_REAUTHORIZED";
    public static final String CONNECTION_UPDATED = "CONNECTION_UPDATED";

    private ConnectionAuditEvents() {
    }
}

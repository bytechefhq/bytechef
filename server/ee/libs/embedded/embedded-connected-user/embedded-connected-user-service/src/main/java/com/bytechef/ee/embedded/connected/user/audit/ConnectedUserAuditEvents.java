/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ConnectedUserAuditEvents {

    public static final String CONNECTED_USERS_IMPORTED = "CONNECTED_USERS_IMPORTED";
    public static final String CONNECTED_USER_CREATED = "CONNECTED_USER_CREATED";
    public static final String CONNECTED_USER_DELETED = "CONNECTED_USER_DELETED";
    public static final String CONNECTED_USER_ENABLED_UPDATED = "CONNECTED_USER_ENABLED_UPDATED";
    public static final String CONNECTED_USER_UPDATED = "CONNECTED_USER_UPDATED";

    private ConnectedUserAuditEvents() {
    }
}

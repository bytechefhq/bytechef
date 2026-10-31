/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

/**
 * Audit event names for embedded membership changes. Strings, not an enum, because {@code @Audited#event()} needs a
 * compile-time constant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EmbeddedUserAuditEvents {

    public static final String EMBEDDED_USER_ADDED = "EMBEDDED_USER_ADDED";
    public static final String EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED = "EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED";
    public static final String EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED = "EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED";
    public static final String EMBEDDED_USER_REMOVED = "EMBEDDED_USER_REMOVED";
    public static final String EMBEDDED_USER_ROLE_UPDATED = "EMBEDDED_USER_ROLE_UPDATED";

    private EmbeddedUserAuditEvents() {
    }
}

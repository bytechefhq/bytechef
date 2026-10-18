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
public final class ProjectAuditEvents {

    public static final String PROJECT_ACCESS_GRANTED = "PROJECT_ACCESS_GRANTED";
    public static final String PROJECT_ACCESS_REVOKED = "PROJECT_ACCESS_REVOKED";
    public static final String PROJECT_VISIBILITY_CHANGED = "PROJECT_VISIBILITY_CHANGED";

    private ProjectAuditEvents() {
    }
}

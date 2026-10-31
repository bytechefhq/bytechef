/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ScimUserAuditEvents {

    public static final String SCIM_USER_CREATED = "SCIM_USER_CREATED";
    public static final String SCIM_USER_DEACTIVATED = "SCIM_USER_DEACTIVATED";
    public static final String SCIM_USER_DELETED = "SCIM_USER_DELETED";
    public static final String SCIM_USER_PATCHED = "SCIM_USER_PATCHED";
    public static final String SCIM_USER_UPDATED = "SCIM_USER_UPDATED";

    private ScimUserAuditEvents() {
    }
}

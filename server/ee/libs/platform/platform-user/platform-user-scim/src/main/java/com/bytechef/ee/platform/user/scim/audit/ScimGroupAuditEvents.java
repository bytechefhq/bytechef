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
public final class ScimGroupAuditEvents {

    public static final String SCIM_GROUP_CREATED = "SCIM_GROUP_CREATED";
    public static final String SCIM_GROUP_DELETED = "SCIM_GROUP_DELETED";
    public static final String SCIM_GROUP_UPDATED = "SCIM_GROUP_UPDATED";

    private ScimGroupAuditEvents() {
    }
}

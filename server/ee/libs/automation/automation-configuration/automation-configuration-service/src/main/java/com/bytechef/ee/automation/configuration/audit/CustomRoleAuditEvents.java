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
public final class CustomRoleAuditEvents {

    public static final String CUSTOM_ROLE_CREATED = "CUSTOM_ROLE_CREATED";
    public static final String CUSTOM_ROLE_DELETED = "CUSTOM_ROLE_DELETED";
    public static final String CUSTOM_ROLE_UPDATED = "CUSTOM_ROLE_UPDATED";

    private CustomRoleAuditEvents() {
    }
}

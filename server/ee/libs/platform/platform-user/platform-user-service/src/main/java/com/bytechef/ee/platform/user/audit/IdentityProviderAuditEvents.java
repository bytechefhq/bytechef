/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class IdentityProviderAuditEvents {

    public static final String IDENTITY_PROVIDER_CREATED = "IDENTITY_PROVIDER_CREATED";
    public static final String IDENTITY_PROVIDER_DELETED = "IDENTITY_PROVIDER_DELETED";
    public static final String IDENTITY_PROVIDER_UPDATED = "IDENTITY_PROVIDER_UPDATED";

    private IdentityProviderAuditEvents() {
    }
}

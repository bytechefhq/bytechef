/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SigningKeyAuditEvents {

    public static final String SIGNING_KEY_CREATED = "SIGNING_KEY_CREATED";
    public static final String SIGNING_KEY_DELETED = "SIGNING_KEY_DELETED";
    public static final String SIGNING_KEY_UPDATED = "SIGNING_KEY_UPDATED";

    private SigningKeyAuditEvents() {
    }
}

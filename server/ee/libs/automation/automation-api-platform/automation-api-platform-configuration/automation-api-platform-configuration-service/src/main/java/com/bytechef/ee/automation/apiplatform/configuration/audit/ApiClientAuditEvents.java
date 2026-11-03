/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ApiClientAuditEvents {

    public static final String API_CLIENT_CREATED = "API_CLIENT_CREATED";
    public static final String API_CLIENT_DELETED = "API_CLIENT_DELETED";
    public static final String API_CLIENT_UPDATED = "API_CLIENT_UPDATED";

    private ApiClientAuditEvents() {
    }
}

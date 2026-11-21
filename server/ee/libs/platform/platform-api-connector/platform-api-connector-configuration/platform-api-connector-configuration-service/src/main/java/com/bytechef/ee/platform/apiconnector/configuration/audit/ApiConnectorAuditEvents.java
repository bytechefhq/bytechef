/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ApiConnectorAuditEvents {

    public static final String API_CONNECTOR_CREATED = "API_CONNECTOR_CREATED";
    public static final String API_CONNECTOR_DELETED = "API_CONNECTOR_DELETED";
    public static final String API_CONNECTOR_ENABLED_UPDATED = "API_CONNECTOR_ENABLED_UPDATED";
    public static final String API_CONNECTOR_GENERATED = "API_CONNECTOR_GENERATED";
    public static final String API_CONNECTOR_GENERATION_JOB_CANCELLED = "API_CONNECTOR_GENERATION_JOB_CANCELLED";
    public static final String API_CONNECTOR_GENERATION_JOB_STARTED = "API_CONNECTOR_GENERATION_JOB_STARTED";
    public static final String API_CONNECTOR_IMPORTED = "API_CONNECTOR_IMPORTED";
    public static final String API_CONNECTOR_UPDATED = "API_CONNECTOR_UPDATED";

    private ApiConnectorAuditEvents() {
    }
}

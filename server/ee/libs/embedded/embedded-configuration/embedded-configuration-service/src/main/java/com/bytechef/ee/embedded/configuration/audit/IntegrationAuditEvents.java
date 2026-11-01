/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.audit;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class IntegrationAuditEvents {

    public static final String INTEGRATION_CREATED = "INTEGRATION_CREATED";
    public static final String INTEGRATION_DELETED = "INTEGRATION_DELETED";
    public static final String INTEGRATION_INSTANCE_CONFIGURATION_CREATED =
        "INTEGRATION_INSTANCE_CONFIGURATION_CREATED";
    public static final String INTEGRATION_INSTANCE_CONFIGURATION_DELETED =
        "INTEGRATION_INSTANCE_CONFIGURATION_DELETED";
    public static final String INTEGRATION_INSTANCE_CONFIGURATION_ENABLED_UPDATED =
        "INTEGRATION_INSTANCE_CONFIGURATION_ENABLED_UPDATED";
    public static final String INTEGRATION_INSTANCE_CONFIGURATION_UPDATED =
        "INTEGRATION_INSTANCE_CONFIGURATION_UPDATED";
    public static final String INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED =
        "INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED";
    public static final String INTEGRATION_INSTANCE_DELETED = "INTEGRATION_INSTANCE_DELETED";
    public static final String INTEGRATION_INSTANCE_ENABLED_UPDATED = "INTEGRATION_INSTANCE_ENABLED_UPDATED";
    public static final String INTEGRATION_INSTANCE_PROVISIONED = "INTEGRATION_INSTANCE_PROVISIONED";
    public static final String INTEGRATION_INSTANCE_WORKFLOW_ENABLED_UPDATED =
        "INTEGRATION_INSTANCE_WORKFLOW_ENABLED_UPDATED";
    public static final String INTEGRATION_PUBLISHED = "INTEGRATION_PUBLISHED";
    public static final String INTEGRATION_UPDATED = "INTEGRATION_UPDATED";

    private IntegrationAuditEvents() {
    }
}

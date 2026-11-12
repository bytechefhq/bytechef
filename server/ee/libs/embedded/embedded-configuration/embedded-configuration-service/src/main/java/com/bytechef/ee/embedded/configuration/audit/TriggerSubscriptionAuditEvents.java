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
public final class TriggerSubscriptionAuditEvents {

    public static final String TRIGGER_SUBSCRIPTION_CREATED = "TRIGGER_SUBSCRIPTION_CREATED";
    public static final String TRIGGER_SUBSCRIPTION_DELETED = "TRIGGER_SUBSCRIPTION_DELETED";
    public static final String TRIGGER_SUBSCRIPTION_ENABLED_UPDATED = "TRIGGER_SUBSCRIPTION_ENABLED_UPDATED";
    public static final String TRIGGER_SUBSCRIPTION_SIGNING_SECRET_ROTATED =
        "TRIGGER_SUBSCRIPTION_SIGNING_SECRET_ROTATED";
    public static final String TRIGGER_SUBSCRIPTION_TEST_EVENT_SENT = "TRIGGER_SUBSCRIPTION_TEST_EVENT_SENT";
    public static final String TRIGGER_SUBSCRIPTION_UPDATED = "TRIGGER_SUBSCRIPTION_UPDATED";
    public static final String TRIGGER_SUBSCRIPTION_WEBHOOK_URL_UPDATED = "TRIGGER_SUBSCRIPTION_WEBHOOK_URL_UPDATED";

    private TriggerSubscriptionAuditEvents() {
    }
}

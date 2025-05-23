/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.aws.constant;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsTriggerSchedulerConstants {

    public static final String DYNAMIC_WEBHOOK_TRIGGER_REFRESH = "DynamicWebhookTriggerRefresh";
    public static final String SPLITTER = "_;_";
    public static final String SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE =
        "scheduler-dynamic_webhook_trigger_refresh_queue";
    public static final String SCHEDULER_POLLING_TRIGGER_QUEUE = "scheduler-polling_trigger_queue";
    public static final String SCHEDULER_SCHEDULE_TRIGGER_QUEUE = "scheduler-schedule_trigger_queue";
}

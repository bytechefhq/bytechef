/*
 * Copyright 2023-present ByteChef Inc.
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
    public static final String SPLITTER_PATTERN = "\\|_\\$plitter_\\|";
    public static final String TRIGGER_SCHEDULER_DYNAMIC_WEBHOOK_TRIGGER_REFRESH_QUEUE =
        "trigger-scheduler-dynamic-webhook-trigger-refresh-queue";
    public static final String TRIGGER_SCHEDULER_POLLING_TRIGGER_QUEUE = "trigger-scheduler-polling-trigger-queue";
    public static final String TRIGGER_SCHEDULER_SCHEDULE_TRIGGER_QUEUE = "trigger-scheduler-schedule-trigger-queue";
}

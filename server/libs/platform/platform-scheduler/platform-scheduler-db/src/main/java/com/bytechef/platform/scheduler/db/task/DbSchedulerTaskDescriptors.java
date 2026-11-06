/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.scheduler.db.task;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;

/**
 * @author Ivica Cardic
 */
public final class DbSchedulerTaskDescriptors {

    public static final String SCHEDULE_TRIGGER_NAME = "schedule-trigger";
    public static final String POLLING_TRIGGER_NAME = "polling-trigger";
    public static final String DYNAMIC_WEBHOOK_REFRESH_NAME = "dynamic-webhook-refresh";
    public static final String OAUTH2_TOKEN_REFRESH_NAME = "oauth2-token-refresh";
    public static final String ONE_TIME_RESUME_NAME = "one-time-resume";
    public static final String STRIPE_USAGE_REPORT_NAME = "stripe-usage-report";
    public static final String QUARTZ_IMPORT_NAME = "quartz-import";

    public static final TaskDescriptor<ScheduleTriggerData> SCHEDULE_TRIGGER =
        TaskDescriptor.of(SCHEDULE_TRIGGER_NAME, ScheduleTriggerData.class);
    public static final TaskDescriptor<PollingTriggerData> POLLING_TRIGGER =
        TaskDescriptor.of(POLLING_TRIGGER_NAME, PollingTriggerData.class);
    public static final TaskDescriptor<DynamicWebhookRefreshData> DYNAMIC_WEBHOOK_REFRESH =
        TaskDescriptor.of(DYNAMIC_WEBHOOK_REFRESH_NAME, DynamicWebhookRefreshData.class);
    public static final TaskDescriptor<OAuth2TokenRefreshData> OAUTH2_TOKEN_REFRESH =
        TaskDescriptor.of(OAUTH2_TOKEN_REFRESH_NAME, OAuth2TokenRefreshData.class);
    public static final TaskDescriptor<OneTimeResumeData> ONE_TIME_RESUME =
        TaskDescriptor.of(ONE_TIME_RESUME_NAME, OneTimeResumeData.class);
    public static final TaskDescriptor<Void> STRIPE_USAGE_REPORT = TaskDescriptor.of(STRIPE_USAGE_REPORT_NAME);
    public static final TaskDescriptor<Void> QUARTZ_IMPORT = TaskDescriptor.of(QUARTZ_IMPORT_NAME);

    private DbSchedulerTaskDescriptors() {
    }
}

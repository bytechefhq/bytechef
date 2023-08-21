
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.scheduler.trigger.constant;

import com.bytechef.hermes.scheduler.trigger.data.PollingTriggerScheduleAndData;
import com.bytechef.hermes.scheduler.trigger.data.ScheduleTriggerScheduleAndData;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.TaskWithDataDescriptor;

/**
 * @author Ivica Cardic
 */
public class TriggerSchedulerConstants {

    public static final TaskWithDataDescriptor<PollingTriggerScheduleAndData> POLLING_TRIGGER_RECURRING_TASK =
        new TaskWithDataDescriptor<>(
            "polling-trigger-recurring-task", PollingTriggerScheduleAndData.class);

    public static final TaskWithDataDescriptor<WorkflowExecutionId> DYNAMIC_WEBHOOK_TRIGGER_REFRESH_ONE_TIME_TASK =
        new TaskWithDataDescriptor<>(
            "dynamic-webhook-trigger-refresh-one-time-task", WorkflowExecutionId.class);

    public static final TaskWithDataDescriptor<ScheduleTriggerScheduleAndData> SCHEDULE_TRIGGER_RECURRING_TASK =
        new TaskWithDataDescriptor<>(
            "schedule-trigger-recurring-task", ScheduleTriggerScheduleAndData.class);
}

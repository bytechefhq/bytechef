
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

package com.bytechef.hermes.scheduler.constant;

import com.bytechef.hermes.scheduler.data.PollTriggerScheduleAndData;
import com.bytechef.hermes.scheduler.data.TriggerWorkflowScheduleAndData;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.TaskWithDataDescriptor;

/**
 * @author Ivica Cardic
 */
public class SchedulerConstants {

    public static final TaskWithDataDescriptor<PollTriggerScheduleAndData> POLL_TRIGGER_RECURRING_TASK =
        new TaskWithDataDescriptor<>(
            "poll-trigger-recurring-task", PollTriggerScheduleAndData.class);

    public static final TaskWithDataDescriptor<WorkflowExecutionId> REFRESH_DYNAMIC_WEBHOOK_TRIGGER_ONE_TIME_TASK =
        new TaskWithDataDescriptor<>(
            "dynamic-webhook-trigger-refresh-recurring-task", WorkflowExecutionId.class);

    public static final TaskWithDataDescriptor<TriggerWorkflowScheduleAndData> TRIGGER_WORKFLOW_RECURRING_TASK =
        new TaskWithDataDescriptor<>("trigger-workflow-recurring-task", TriggerWorkflowScheduleAndData.class);
}

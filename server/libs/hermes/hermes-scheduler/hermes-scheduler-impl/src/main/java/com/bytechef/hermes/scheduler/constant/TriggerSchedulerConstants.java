
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

import com.bytechef.hermes.scheduler.data.PollScheduleAndData;
import com.bytechef.hermes.scheduler.data.ExecuteWorkflowScheduleAndData;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.TaskWithDataDescriptor;

/**
 * @author Ivica Cardic
 */
public class TriggerSchedulerConstants {

    public static final TaskWithDataDescriptor<PollScheduleAndData> TRIGGER_POLL_RECURRING_TASK =
        new TaskWithDataDescriptor<>(
            "trigger-poll-recurring-task", PollScheduleAndData.class);

    public static final TaskWithDataDescriptor<WorkflowExecutionId> TRIGGER_REFRESH_DYNAMIC_WEBHOOK_ONE_TIME_TASK =
        new TaskWithDataDescriptor<>(
            "trigger-refresh-dynamic-webhook-recurring-task", WorkflowExecutionId.class);

    public static final TaskWithDataDescriptor<ExecuteWorkflowScheduleAndData> TRIGGER_EXECUTE_WORKFLOW_RECURRING_TASK =
        new TaskWithDataDescriptor<>(
            "trigger-execute-workflow-recurring-task", ExecuteWorkflowScheduleAndData.class);
}

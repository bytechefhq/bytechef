
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

package com.bytechef.hermes.scheduler;

import com.bytechef.commons.util.InstantUtils;
import com.bytechef.hermes.scheduler.constant.TriggerScheduleConstants;
import com.bytechef.hermes.scheduler.data.TriggerScheduleAndData;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSchedulerImpl implements TriggerScheduler {

    private final SchedulerClient schedulerClient;

    public TriggerSchedulerImpl(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void cancelDynamicWebhookRefreshTask(WorkflowExecutionId workflowExecutionId) {
        schedulerClient.cancel(
            TaskInstanceId.of(
                TriggerScheduleConstants.TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK.getTaskName(),
                workflowExecutionId.toString()));
    }

    @Override
    public void cancelPollTask(WorkflowExecutionId workflowExecutionId) {
        schedulerClient.cancel(
            TaskInstanceId.of(
                TriggerScheduleConstants.TRIGGER_POLL_RECURRING_TASK.getTaskName(), workflowExecutionId.toString()));
    }

    @Override
    public void scheduleDynamicWebhookRefreshTask(
        WorkflowTrigger workflowTrigger, WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate) {

        schedulerClient.schedule(
            TriggerScheduleConstants.TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK.instance(
                workflowExecutionId.toString(), new TriggerScheduleAndData(workflowTrigger, workflowExecutionId)),
            InstantUtils.getInstant(webhookExpirationDate));
    }

    @Override
    public void schedulePollTask(WorkflowExecutionId workflowExecutionId) {
        Schedule schedule = FixedDelay.ofMinutes(5);

        schedulerClient.schedule(
            TriggerScheduleConstants.TRIGGER_POLL_RECURRING_TASK.instance(
                workflowExecutionId.toString(), new TriggerScheduleAndData(schedule, workflowExecutionId)),
            schedule.getInitialExecutionTime(Instant.now()));
    }
}

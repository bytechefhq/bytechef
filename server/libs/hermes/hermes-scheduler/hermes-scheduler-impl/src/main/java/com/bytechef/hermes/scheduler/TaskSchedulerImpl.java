
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
import com.bytechef.hermes.scheduler.data.PollTriggerScheduleAndData;
import com.bytechef.hermes.scheduler.data.TriggerWorkflowScheduleAndData;
import com.bytechef.hermes.scheduler.data.RefreshDynamicWebhookTriggerData;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static com.bytechef.hermes.scheduler.constant.SchedulerConstants.REFRESH_DYNAMIC_WEBHOOK_TRIGGER_ONE_TIME_TASK;
import static com.bytechef.hermes.scheduler.constant.SchedulerConstants.POLL_TRIGGER_RECURRING_TASK;
import static com.bytechef.hermes.scheduler.constant.SchedulerConstants.TRIGGER_WORKFLOW_RECURRING_TASK;

/**
 * @author Ivica Cardic
 */
@Component
public class TaskSchedulerImpl implements TaskScheduler {

    private final SchedulerClient schedulerClient;

    public TaskSchedulerImpl(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void cancelRefreshDynamicWebhookTriggerTask(String workflowExecutionId) {
        schedulerClient.cancel(
            TaskInstanceId.of(REFRESH_DYNAMIC_WEBHOOK_TRIGGER_ONE_TIME_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void cancelPollTriggerTask(String workflowExecutionId) {
        schedulerClient.cancel(TaskInstanceId.of(POLL_TRIGGER_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void cancelTriggerWorkflowTask(String workflowExecutionId) {
        schedulerClient.cancel(TaskInstanceId.of(TRIGGER_WORKFLOW_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void scheduleRefreshDynamicWebhookTriggerTask(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {

        schedulerClient.schedule(
            REFRESH_DYNAMIC_WEBHOOK_TRIGGER_ONE_TIME_TASK.instance(
                workflowExecutionId.toString(),
                new RefreshDynamicWebhookTriggerData(workflowExecutionId, componentName, componentVersion)),
            InstantUtils.getInstant(webhookExpirationDate));
    }

    @Override
    public void schedulePollTriggerTask(WorkflowExecutionId workflowExecutionId) {
        Schedule schedule = FixedDelay.ofMinutes(5);

        schedulerClient.schedule(
            POLL_TRIGGER_RECURRING_TASK.instance(
                workflowExecutionId.toString(), new PollTriggerScheduleAndData(schedule, workflowExecutionId)),
            schedule.getInitialExecutionTime(Instant.now()));
    }

    @Override
    public void scheduleTriggerWorkflowTask(
        String workflowExecutionId, String pattern, String zoneId, Map<String, Object> output) {

        CronSchedule cronSchedule = new CronSchedule(pattern, ZoneId.of(zoneId));

        schedulerClient.schedule(
            TRIGGER_WORKFLOW_RECURRING_TASK.instance(
                workflowExecutionId,
                new TriggerWorkflowScheduleAndData(cronSchedule, workflowExecutionId, output)),
            cronSchedule.getInitialExecutionTime(Instant.now()));
    }
}

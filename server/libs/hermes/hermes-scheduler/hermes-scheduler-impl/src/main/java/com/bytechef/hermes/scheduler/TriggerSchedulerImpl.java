
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
import com.bytechef.hermes.scheduler.data.PollScheduleAndData;
import com.bytechef.hermes.scheduler.data.ExecuteWorkflowScheduleAndData;
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

import static com.bytechef.hermes.scheduler.constant.TriggerSchedulerConstants.TRIGGER_REFRESH_DYNAMIC_WEBHOOK_ONE_TIME_TASK;
import static com.bytechef.hermes.scheduler.constant.TriggerSchedulerConstants.TRIGGER_POLL_RECURRING_TASK;
import static com.bytechef.hermes.scheduler.constant.TriggerSchedulerConstants.TRIGGER_EXECUTE_WORKFLOW_RECURRING_TASK;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSchedulerImpl implements TriggerScheduler {

    private final SchedulerClient schedulerClient;

    public TriggerSchedulerImpl(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void cancelDynamicWebhookRefreshTask(String workflowExecutionId) {
        schedulerClient.cancel(
            TaskInstanceId.of(TRIGGER_REFRESH_DYNAMIC_WEBHOOK_ONE_TIME_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void cancelExecuteWorkflowTask(String workflowExecutionId) {
        schedulerClient
            .cancel(TaskInstanceId.of(TRIGGER_EXECUTE_WORKFLOW_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void cancelPollTask(String workflowExecutionId) {
        schedulerClient.cancel(TaskInstanceId.of(TRIGGER_POLL_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void scheduleDynamicWebhookRefreshTask(
        WorkflowExecutionId workflowExecutionId, LocalDateTime webhookExpirationDate, String componentName,
        int componentVersion) {

        schedulerClient.schedule(
            TRIGGER_REFRESH_DYNAMIC_WEBHOOK_ONE_TIME_TASK.instance(workflowExecutionId.toString(), workflowExecutionId),
            InstantUtils.getInstant(webhookExpirationDate));
    }

    @Override
    public void scheduleExecuteWorkflowTask(
        String workflowExecutionId, String pattern, String zoneId, Map<String, Object> output) {

        CronSchedule cronSchedule = new CronSchedule(pattern, ZoneId.of(zoneId));

        schedulerClient.schedule(
            TRIGGER_EXECUTE_WORKFLOW_RECURRING_TASK.instance(
                workflowExecutionId,
                new ExecuteWorkflowScheduleAndData(cronSchedule, workflowExecutionId, output)),
            cronSchedule.getInitialExecutionTime(Instant.now()));
    }

    @Override
    public void schedulePollTask(WorkflowExecutionId workflowExecutionId) {
        Schedule schedule = FixedDelay.ofMinutes(5);

        schedulerClient.schedule(
            TRIGGER_POLL_RECURRING_TASK.instance(
                workflowExecutionId.toString(), new PollScheduleAndData(schedule, workflowExecutionId)),
            schedule.getInitialExecutionTime(Instant.now()));
    }
}

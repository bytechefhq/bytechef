
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

package com.bytechef.hermes.scheduler.trigger;

import com.bytechef.commons.util.InstantUtils;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import com.bytechef.hermes.scheduler.trigger.data.PollingTriggerScheduleAndData;
import com.bytechef.hermes.scheduler.trigger.data.ScheduleTriggerScheduleAndData;
import com.bytechef.hermes.configuration.WorkflowExecutionId;
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

import static com.bytechef.hermes.scheduler.trigger.constant.TriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH_ONE_TIME_TASK;
import static com.bytechef.hermes.scheduler.trigger.constant.TriggerSchedulerConstants.POLLING_TRIGGER_RECURRING_TASK;
import static com.bytechef.hermes.scheduler.trigger.constant.TriggerSchedulerConstants.SCHEDULE_TRIGGER_RECURRING_TASK;

/**
 * @author Ivica Cardic
 */
@Component
public class TriggerSchedulerImpl implements TriggerScheduler {

    private final SchedulerClient schedulerClient;

    public TriggerSchedulerImpl(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    public void cancelDynamicWebhookTriggerRefresh(String workflowExecutionId) {
        schedulerClient.cancel(
            TaskInstanceId.of(DYNAMIC_WEBHOOK_TRIGGER_REFRESH_ONE_TIME_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void cancelScheduleTrigger(String workflowExecutionId) {
        schedulerClient
            .cancel(TaskInstanceId.of(SCHEDULE_TRIGGER_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void cancelPollingTrigger(String workflowExecutionId) {
        schedulerClient.cancel(TaskInstanceId.of(POLLING_TRIGGER_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }

    @Override
    public void scheduleDynamicWebhookTriggerRefresh(
        LocalDateTime webhookExpirationDate, String componentName, int componentVersion,
        WorkflowExecutionId workflowExecutionId) {

        schedulerClient.schedule(
            DYNAMIC_WEBHOOK_TRIGGER_REFRESH_ONE_TIME_TASK.instance(workflowExecutionId.toString(), workflowExecutionId),
            InstantUtils.getInstant(webhookExpirationDate));
    }

    @Override
    public void scheduleScheduleTrigger(
        String pattern, String zoneId, Map<String, Object> output, String workflowExecutionId) {

        CronSchedule cronSchedule = new CronSchedule(pattern, ZoneId.of(zoneId));

        schedulerClient.schedule(
            SCHEDULE_TRIGGER_RECURRING_TASK.instance(
                workflowExecutionId,
                new ScheduleTriggerScheduleAndData(cronSchedule, workflowExecutionId, output)),
            cronSchedule.getInitialExecutionTime(Instant.now()));
    }

    @Override
    public void schedulePollingTrigger(WorkflowExecutionId workflowExecutionId) {
        Schedule schedule = FixedDelay.ofMinutes(5);

        schedulerClient.schedule(
            POLLING_TRIGGER_RECURRING_TASK.instance(
                workflowExecutionId.toString(), new PollingTriggerScheduleAndData(schedule, workflowExecutionId)),
            schedule.getInitialExecutionTime(Instant.now()));
    }
}

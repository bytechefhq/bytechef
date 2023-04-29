
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

package com.bytechef.hermes.trigger.executor.config;

import com.bytechef.commons.util.InstantUtils;
import com.bytechef.hermes.trigger.executor.data.TriggerScheduleAndData;
import com.bytechef.hermes.trigger.executor.ScheduledTriggerExecutor;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

import static com.bytechef.hermes.trigger.executor.constants.TriggerScheduleConstants.TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK;
import static com.bytechef.hermes.trigger.executor.constants.TriggerScheduleConstants.TRIGGER_POLL_RECURRING_TASK;

@Configuration
public class TriggerScheduleConfiguration {

    private final ApplicationContext applicationContext;

    public TriggerScheduleConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    Task<TriggerScheduleAndData> triggerPollTask() {
        return Tasks.recurringWithPersistentSchedule(TRIGGER_POLL_RECURRING_TASK)
            .execute((TaskInstance<TriggerScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                TriggerScheduleAndData triggerScheduleAndData = taskInstance.getData();

                TriggerScheduleAndData.Data data = triggerScheduleAndData.getData();

                ScheduledTriggerExecutor scheduledTriggerExecutor = applicationContext.getBean(
                    ScheduledTriggerExecutor.class);

                scheduledTriggerExecutor.executeTriggerPoll(
                    data.workflowTrigger(), data.workflowExecutionId(), data.context());
            });
    }

    @Bean
    Task<TriggerScheduleAndData> triggerDynamicWebhookRefreshTask() {
        return Tasks.oneTime(TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK)
            .execute((taskInstance, executionContext) -> {
                TriggerScheduleAndData triggerScheduleAndData = taskInstance.getData();

                TriggerScheduleAndData.Data data = triggerScheduleAndData.getData();

                WorkflowExecutionId workflowExecutionId = data.workflowExecutionId();

                ScheduledTriggerExecutor scheduledTriggerExecutor = applicationContext.getBean(
                    ScheduledTriggerExecutor.class);

                LocalDateTime webhookExpirationDate = scheduledTriggerExecutor.executeTriggerDynamicWebhookRefresh(
                    data.workflowTrigger(), workflowExecutionId);

                if (webhookExpirationDate != null) {
                    SchedulerClient schedulerClient = executionContext.getSchedulerClient();

                    schedulerClient.schedule(
                        TRIGGER_DYNAMIC_WEBHOOK_REFRESH_ONE_TIME_TASK.instance(
                            workflowExecutionId.toString(),
                            new TriggerScheduleAndData(workflowExecutionId, data.workflowTrigger())),
                        InstantUtils.getInstant(webhookExpirationDate));
                }
            });
    }
}

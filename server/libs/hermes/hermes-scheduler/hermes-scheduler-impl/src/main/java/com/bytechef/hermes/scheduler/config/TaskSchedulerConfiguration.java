
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

package com.bytechef.hermes.scheduler.config;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.InstantUtils;
import com.bytechef.hermes.scheduler.constant.SchedulerConstants;
import com.bytechef.hermes.scheduler.data.PollTriggerScheduleAndData;
import com.bytechef.hermes.scheduler.data.RefreshDynamicWebhookTriggerData;
import com.bytechef.hermes.scheduler.data.TriggerWorkflowScheduleAndData;
import com.bytechef.hermes.scheduler.executor.ScheduledTaskExecutor;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class TaskSchedulerConfiguration {

    private final ApplicationContext applicationContext;

    public TaskSchedulerConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    Task<PollTriggerScheduleAndData> pollTriggerTask() {
        return Tasks.recurringWithPersistentSchedule(SchedulerConstants.POLL_TRIGGER_RECURRING_TASK)
            .execute((TaskInstance<PollTriggerScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                PollTriggerScheduleAndData triggerScheduleAndData = taskInstance.getData();

                PollTriggerScheduleAndData.Data data = triggerScheduleAndData.getData();

                ScheduledTaskExecutor scheduledTaskExecutor = applicationContext.getBean(
                    ScheduledTaskExecutor.class);

                scheduledTaskExecutor.pollTrigger(data.workflowExecutionId());
            });
    }

    @Bean
    Task<RefreshDynamicWebhookTriggerData> refreshDynamicWebhookTriggerTask() {
        return Tasks.oneTime(SchedulerConstants.REFRESH_DYNAMIC_WEBHOOK_TRIGGER_ONE_TIME_TASK)
            .execute((taskInstance, executionContext) -> {
                RefreshDynamicWebhookTriggerData triggerScheduleAndData = taskInstance.getData();

                RefreshDynamicWebhookTriggerData.Data data = triggerScheduleAndData.getData();

                WorkflowExecutionId workflowExecutionId = data.workflowExecutionId();

                ScheduledTaskExecutor scheduledTaskExecutor = applicationContext.getBean(
                    ScheduledTaskExecutor.class);

                LocalDateTime webhookExpirationDate = scheduledTaskExecutor.refreshDynamicWebhookTrigger(
                    workflowExecutionId, data.componentName(), data.componentVersion());

                if (webhookExpirationDate != null) {
                    SchedulerClient schedulerClient = executionContext.getSchedulerClient();

                    schedulerClient.reschedule(
                        SchedulerConstants.REFRESH_DYNAMIC_WEBHOOK_TRIGGER_ONE_TIME_TASK.instance(
                            workflowExecutionId.toString(),
                            new RefreshDynamicWebhookTriggerData(
                                workflowExecutionId, data.componentName(), data.componentVersion())),
                        InstantUtils.getInstant(webhookExpirationDate));
                }
            });
    }

    @Bean
    Task<TriggerWorkflowScheduleAndData> triggerWorkflowTask() {
        return Tasks.recurringWithPersistentSchedule(SchedulerConstants.TRIGGER_WORKFLOW_RECURRING_TASK)
            .execute((TaskInstance<TriggerWorkflowScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                Execution execution = executionContext.getExecution();
                Instant executionTime = execution.getExecutionTime();

                TriggerWorkflowScheduleAndData triggerWorkflowScheduleAndData = taskInstance.getData();

                TriggerWorkflowScheduleAndData.Data data = triggerWorkflowScheduleAndData.getData();

                ScheduledTaskExecutor scheduledTaskExecutor = applicationContext.getBean(
                    ScheduledTaskExecutor.class);

                scheduledTaskExecutor.triggerWorkflow(
                    WorkflowExecutionId.parse(data.workflowExecutionId()),
                    CollectionUtils.concat(Map.of("datetime", executionTime.toString()), data.output()));
            });
    }
}

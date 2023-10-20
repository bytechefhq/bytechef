
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
import com.bytechef.hermes.scheduler.constant.TriggerSchedulerConstants;
import com.bytechef.hermes.scheduler.data.PollScheduleAndData;
import com.bytechef.hermes.scheduler.data.ExecuteWorkflowScheduleAndData;
import com.bytechef.hermes.scheduler.executor.TriggerSchedulerExecutor;
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
public class TriggerSchedulerConfiguration {

    private final ApplicationContext applicationContext;

    public TriggerSchedulerConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    Task<ExecuteWorkflowScheduleAndData> executeWorkflowTask() {
        return Tasks.recurringWithPersistentSchedule(TriggerSchedulerConstants.TRIGGER_EXECUTE_WORKFLOW_RECURRING_TASK)
            .execute((TaskInstance<ExecuteWorkflowScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                Execution execution = executionContext.getExecution();
                Instant executionTime = execution.getExecutionTime();

                ExecuteWorkflowScheduleAndData executeWorkflowScheduleAndData = taskInstance.getData();

                ExecuteWorkflowScheduleAndData.Data data = executeWorkflowScheduleAndData.getData();

                TriggerSchedulerExecutor triggerSchedulerExecutor = applicationContext.getBean(
                    TriggerSchedulerExecutor.class);

                triggerSchedulerExecutor.executeWorkflow(
                    WorkflowExecutionId.parse(data.workflowExecutionId()),
                    CollectionUtils.concat(Map.of("datetime", executionTime.toString()), data.output()));
            });
    }

    @Bean
    Task<PollScheduleAndData> pollTask() {
        return Tasks.recurringWithPersistentSchedule(TriggerSchedulerConstants.TRIGGER_POLL_RECURRING_TASK)
            .execute((TaskInstance<PollScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                PollScheduleAndData triggerScheduleAndData = taskInstance.getData();

                TriggerSchedulerExecutor scheduledTriggerExecutor = applicationContext.getBean(
                    TriggerSchedulerExecutor.class);

                scheduledTriggerExecutor.poll(triggerScheduleAndData.getData());
            });
    }

    @Bean
    Task<WorkflowExecutionId> refreshDynamicWebhookTask() {
        return Tasks.oneTime(TriggerSchedulerConstants.TRIGGER_REFRESH_DYNAMIC_WEBHOOK_ONE_TIME_TASK)
            .execute((taskInstance, executionContext) -> {
                WorkflowExecutionId workflowExecutionId = taskInstance.getData();

                TriggerSchedulerExecutor triggerSchedulerExecutor = applicationContext.getBean(
                    TriggerSchedulerExecutor.class);

                LocalDateTime webhookExpirationDate = triggerSchedulerExecutor.refreshDynamicWebhook(
                    workflowExecutionId, workflowExecutionId.getComponentName(),
                    workflowExecutionId.getComponentVersion());

                if (webhookExpirationDate != null) {
                    SchedulerClient schedulerClient = executionContext.getSchedulerClient();

                    schedulerClient.reschedule(
                        TriggerSchedulerConstants.TRIGGER_REFRESH_DYNAMIC_WEBHOOK_ONE_TIME_TASK.instance(
                            workflowExecutionId.toString(), workflowExecutionId),
                        InstantUtils.getInstant(webhookExpirationDate));
                }
            });
    }
}

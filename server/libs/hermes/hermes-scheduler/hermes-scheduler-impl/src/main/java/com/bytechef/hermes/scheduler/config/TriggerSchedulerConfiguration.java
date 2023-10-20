
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

import com.bytechef.hermes.scheduler.handler.DynamicWebhookTriggerRefreshTaskVoidExecutionHandler;
import com.bytechef.hermes.scheduler.handler.PollingTriggerTaskVoidExecutionHandler;
import com.bytechef.hermes.scheduler.handler.ScheduleTriggerTaskVoidExecutionHandler;
import com.bytechef.hermes.scheduler.trigger.constant.TriggerSchedulerConstants;
import com.bytechef.hermes.scheduler.trigger.data.PollingTriggerScheduleAndData;
import com.bytechef.hermes.scheduler.trigger.data.ScheduleTriggerScheduleAndData;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TriggerSchedulerConfiguration {

    private final ApplicationContext applicationContext;

    public TriggerSchedulerConfiguration(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    Task<ScheduleTriggerScheduleAndData> scheduleTriggerTask() {
        return Tasks.recurringWithPersistentSchedule(TriggerSchedulerConstants.SCHEDULE_TRIGGER_RECURRING_TASK)
            .execute(new ScheduleTriggerTaskVoidExecutionHandler((applicationContext)));
    }

    @Bean
    Task<PollingTriggerScheduleAndData> pollingTriggerTask() {
        return Tasks.recurringWithPersistentSchedule(TriggerSchedulerConstants.POLLING_TRIGGER_RECURRING_TASK)
            .execute(new PollingTriggerTaskVoidExecutionHandler(applicationContext));
    }

    @Bean
    Task<WorkflowExecutionId> dynamicWebhookTriggerRefreshTask() {
        return Tasks.oneTime(TriggerSchedulerConstants.DYNAMIC_WEBHOOK_TRIGGER_REFRESH_ONE_TIME_TASK)
            .execute(new DynamicWebhookTriggerRefreshTaskVoidExecutionHandler(applicationContext));
    }
}


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

package com.bytechef.component.schedule.config;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.schedule.constant.ScheduleConstants;
import com.bytechef.component.schedule.data.WorkflowScheduleAndData;
import com.bytechef.hermes.component.util.ListenerTriggerUtils;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Map;

import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;

@Configuration
public class ScheduleConfiguration {

    @Bean
    Task<WorkflowScheduleAndData> workflowExecutionTask() {
        return Tasks.recurringWithPersistentSchedule(ScheduleConstants.SCHEDULE_RECURRING_TASK)
            .execute((TaskInstance<WorkflowScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                Execution execution = executionContext.getExecution();
                Instant executionTime = execution.getExecutionTime();

                WorkflowScheduleAndData workflowScheduleAndData = taskInstance.getData();

                WorkflowScheduleAndData.Data data = workflowScheduleAndData.getData();

                ListenerTriggerUtils.emit(
                    data.workflowExecutionId(),
                    CollectionUtils.concat(Map.of(DATETIME, executionTime.toString()), data.output()));
            });
    }
}

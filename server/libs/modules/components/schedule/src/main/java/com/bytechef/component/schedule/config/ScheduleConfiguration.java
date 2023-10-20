
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
import com.bytechef.hermes.component.util.ListenerTriggerUtils;
import com.github.kagkarlsson.scheduler.task.Execution;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.TaskWithDataDescriptor;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;

@Configuration
public class ScheduleConfiguration {

    public static final TaskWithDataDescriptor<WorkflowScheduleAndData> SCHEDULE_RECURRING_TASK =
        new TaskWithDataDescriptor<>(
            "schedule-recurring-task", WorkflowScheduleAndData.class);

    @Bean
    public Task<WorkflowScheduleAndData> workflowExecutionTask() {
        return Tasks.recurringWithPersistentSchedule(SCHEDULE_RECURRING_TASK)
            .execute(
                (TaskInstance<WorkflowScheduleAndData> taskInstance, ExecutionContext executionContext) -> {
                    Execution execution = executionContext.getExecution();
                    Instant executionTime = execution.getExecutionTime();

                    WorkflowScheduleAndData workflowScheduleAndData = taskInstance.getData();

                    ListenerTriggerUtils.emit(
                        workflowScheduleAndData.getData(),
                        CollectionUtils.concat(
                            Map.of(DATETIME, executionTime.toString()), workflowScheduleAndData.getOutput()));
                });
    }

    public static class WorkflowScheduleAndData implements ScheduleAndData {

        private final Map<String, Object> output;
        private final Schedule schedule;
        private final String workflowInstanceId;

        @SuppressFBWarnings("EI")
        public WorkflowScheduleAndData(
            Schedule schedule, Map<String, Object> output, String workflowInstanceId) {

            this.output = output;
            this.schedule = schedule;
            this.workflowInstanceId = workflowInstanceId;
        }

        @Override
        public String getData() {
            return workflowInstanceId;
        }

        public Map<String, Object> getOutput() {
            return new HashMap<>(output);
        }

        @Override
        public Schedule getSchedule() {
            return schedule;
        }
    }
}

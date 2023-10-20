
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

package com.bytechef.component.schedule.trigger;

import com.bytechef.component.schedule.data.WorkflowScheduleAndData;
import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.util.MapValueUtils;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static com.bytechef.component.schedule.constant.ScheduleConstants.SCHEDULE_RECURRING_TASK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.EXPRESSION;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class ScheduleCronTrigger {

    public final TriggerDefinition triggerDefinition = trigger("cron")
        .title("Cron")
        .description("Trigger off based on a custom schedule.")
        .type(TriggerType.LISTENER)
        .properties(
            string(EXPRESSION)
                .label("Expression")
                .description(
                    "The chron schedule expression. Format: [Minute] [Hour] [Day of Month] [Month] [Day of Week]")
                .required(true),
            string(TIMEZONE)
                .label("Timezone")
                .description("The timezone at which the cron expression will be scheduled.")
                .options(ScheduleUtils.getTimeZoneOptions()))
        .outputSchema(
            object()
                .properties(
                    string(DATETIME),
                    string(EXPRESSION),
                    string(TIMEZONE)))
        .listenerEnable(this::listenerEnable)
        .listenerDisable(this::listenerDisable);

    private final SchedulerClient schedulerClient;

    public ScheduleCronTrigger(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    protected void listenerEnable(
        Connection connection, Map<String, ?> inputParameters, String workflowExecutionId) {

        CronSchedule cronSchedule = new CronSchedule(
            "0 " + MapValueUtils.getString(inputParameters, EXPRESSION),
            ZoneId.of(MapValueUtils.getString(inputParameters, TIMEZONE)));

        schedulerClient.schedule(
            SCHEDULE_RECURRING_TASK.instance(
                workflowExecutionId,
                new WorkflowScheduleAndData(
                    cronSchedule,
                    Map.of(
                        EXPRESSION, MapValueUtils.getString(inputParameters, EXPRESSION),
                        TIMEZONE, MapValueUtils.getString(inputParameters, TIMEZONE)),
                    workflowExecutionId)),
            cronSchedule.getInitialExecutionTime(Instant.now()));
    }

    protected void listenerDisable(
        Connection connection, Map<String, ?> inputParameters, String workflowExecutionId) {

        schedulerClient.cancel(TaskInstanceId.of(SCHEDULE_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }
}

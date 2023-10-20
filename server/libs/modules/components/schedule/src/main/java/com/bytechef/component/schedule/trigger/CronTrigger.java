
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

import com.bytechef.component.schedule.config.ScheduleConfiguration.WorkflowScheduleAndData;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static com.bytechef.component.schedule.config.ScheduleConfiguration.SCHEDULE_RECURRING_TASK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.EXPRESSION;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class CronTrigger {

    public final TriggerDefinition triggerDefinition = trigger("cron")
        .display(display("Cron")
            .description("Schedule workflows to execute based on a custom schedule."))
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
//                .options(
//                    ZoneId.getAvailableZoneIds()
//                        .stream()
//                        .map(zoneId -> option(zoneId, zoneId))
//                        .toList())
        )
        .outputSchema(
            object()
                .properties(
                    string(DATETIME),
                    string(EXPRESSION),
                    string(TIMEZONE)))
        .listenerEnable(this::listenerEnable)
        .listenerDisable(this::listenerDisable);

    private final SchedulerClient schedulerClient;

    public CronTrigger(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    protected void listenerEnable(
        Connection connection, InputParameters inputParameters, String workflowInstanceId) {

        CronSchedule cron = new CronSchedule(
            "0 " + inputParameters.getString(EXPRESSION), ZoneId.of(inputParameters.getString(TIMEZONE)));

        schedulerClient.schedule(
            SCHEDULE_RECURRING_TASK.instance(
                workflowInstanceId,
                new WorkflowScheduleAndData(
                    cron,
                    Map.of(
                        EXPRESSION, inputParameters.getString(EXPRESSION),
                        TIMEZONE, inputParameters.getString(TIMEZONE)),
                    workflowInstanceId)),
            cron.getInitialExecutionTime(Instant.now()));
    }

    protected void listenerDisable(
        Connection connection, InputParameters inputParameters, String workflowInstanceId) {

        schedulerClient.cancel(TaskInstanceId.of(SCHEDULE_RECURRING_TASK.getTaskName(), workflowInstanceId));
    }
}

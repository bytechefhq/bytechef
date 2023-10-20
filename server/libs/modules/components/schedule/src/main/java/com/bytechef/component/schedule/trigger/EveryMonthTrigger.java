
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

import com.bytechef.component.schedule.config.ScheduleConfiguration;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import static com.bytechef.component.schedule.config.ScheduleConfiguration.SCHEDULE_RECURRING_TASK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DAY_OF_MONTH;
import static com.bytechef.component.schedule.constant.ScheduleConstants.HOUR;
import static com.bytechef.component.schedule.constant.ScheduleConstants.MINUTE;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class EveryMonthTrigger {

    public final TriggerDefinition triggerDefinition = trigger("everyMonth")
        .display(display("Every month").description(
            "Schedule workflows to execute at a specific time in month."))
        .type(TriggerDefinition.TriggerType.LISTENER)
        .properties(
            integer(HOUR)
                .label("Hour")
                .description("The hour at which a workflow will be triggered.")
                .required(true)
                .defaultValue(0)
                .minValue(0)
                .maxValue(23),
            integer(MINUTE)
                .label("Minute")
                .description("The minute at which a workflow will be triggered.")
                .required(true)
                .defaultValue(0)
                .minValue(0)
                .maxValue(59),
            integer(DAY_OF_MONTH)
                .label("Day of month")
                .description("The day of the month  at which a workflow will be triggered.")
                .required(true)
                .minValue(1)
                .maxValue(31),
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
                    integer(HOUR),
                    integer(MINUTE),
                    integer(DAY_OF_MONTH),
                    string(TIMEZONE)))
        .listenerEnable(this::listenerEnable)
        .listenerDisable(this::listenerDisable);

    private final SchedulerClient schedulerClient;

    public EveryMonthTrigger(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    protected void listenerEnable(
        Connection connection, InputParameters inputParameters, String workflowExecutionId) {

        CronSchedule cron = new CronSchedule(
            "0 %s %s %s * ?".formatted(
                inputParameters.getInteger(MINUTE), inputParameters.getInteger(HOUR),
                inputParameters.getInteger(DAY_OF_MONTH)),
            ZoneId.of(inputParameters.getString(TIMEZONE)));

        schedulerClient.schedule(
            SCHEDULE_RECURRING_TASK.instance(
                workflowExecutionId,
                new ScheduleConfiguration.WorkflowScheduleAndData(
                    cron,
                    Map.of(
                        HOUR, inputParameters.getInteger(HOUR),
                        MINUTE, inputParameters.getInteger(MINUTE),
                        DAY_OF_MONTH, inputParameters.getInteger(DAY_OF_MONTH),
                        TIMEZONE, inputParameters.getString(TIMEZONE)),
                    workflowExecutionId)),
            cron.getInitialExecutionTime(Instant.now()));
    }

    protected void listenerDisable(
        Connection connection, InputParameters inputParameters, String workflowExecutionId) {

        schedulerClient.cancel(TaskInstanceId.of(SCHEDULE_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }
}

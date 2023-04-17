
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
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bytechef.component.schedule.config.ScheduleConfiguration.SCHEDULE_RECURRING_TASK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DAY_OF_WEEK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.HOUR;
import static com.bytechef.component.schedule.constant.ScheduleConstants.MINUTE;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class EveryDayTrigger {

    public final TriggerDefinition triggerDefinition = trigger("everyDay")
        .display(display("Every day").description(
            "Schedule workflows to execute at a specific time either on a daily basis or selected days of the week."))
        .type(TriggerType.LISTENER)
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
            object(DAY_OF_WEEK)
                .label("Day of week")
                .description("Days at which a workflow will be triggered.")
                .properties(
                    bool("1").label("Monday")
                        .defaultValue(true),
                    bool("2").label("Tuesday")
                        .defaultValue(true),
                    bool("3").label("Wednesday")
                        .defaultValue(true),
                    bool("4").label("Thursday")
                        .defaultValue(true),
                    bool("5").label("Friday")
                        .defaultValue(true),
                    bool("6").label("Saturday")
                        .defaultValue(true),
                    bool("7").label("Sunday")
                        .defaultValue(true)),
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
                    object(DAY_OF_WEEK)
                        .properties(
                            bool("1"),
                            bool("2"),
                            bool("3"),
                            bool("4"),
                            bool("5"),
                            bool("6"),
                            bool("7")),
                    string(TIMEZONE)))
        .listenerEnable(this::listenerEnable)
        .listenerDisable(this::listenerDisable);

    private final SchedulerClient schedulerClient;

    public EveryDayTrigger(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    protected void listenerEnable(
        Connection connection, InputParameters inputParameters, String workflowExecutionId) {

        CronSchedule cron = new CronSchedule(
            "0 %s %s ? * %s".formatted(
                inputParameters.getInteger(MINUTE), inputParameters.getInteger(HOUR),
                inputParameters.getMap(DAY_OF_WEEK)
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() == null || !((Boolean) entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(","))),
            ZoneId.of(inputParameters.getString(TIMEZONE)));

        schedulerClient.schedule(
            SCHEDULE_RECURRING_TASK.instance(
                workflowExecutionId,
                new ScheduleConfiguration.WorkflowScheduleAndData(
                    cron,
                    Map.of(
                        HOUR, inputParameters.getInteger(HOUR),
                        MINUTE, inputParameters.getInteger(MINUTE),
                        DAY_OF_WEEK, inputParameters.getMap(DAY_OF_WEEK),
                        TIMEZONE, inputParameters.getString(TIMEZONE)),
                    workflowExecutionId)),
            cron.getInitialExecutionTime(Instant.now()));
    }

    protected void listenerDisable(
        Connection connection, InputParameters inputParameters, String workflowExecutionId) {

        schedulerClient.cancel(TaskInstanceId.of(SCHEDULE_RECURRING_TASK.getTaskName(), workflowExecutionId));
    }
}

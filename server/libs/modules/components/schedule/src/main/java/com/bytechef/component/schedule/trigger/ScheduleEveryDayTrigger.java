/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DAY_OF_WEEK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.FIRE_TIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.HOUR;
import static com.bytechef.component.schedule.constant.ScheduleConstants.MINUTE;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class ScheduleEveryDayTrigger {

    public final ModifiableTriggerDefinition triggerDefinition = trigger("everyDay")
        .title("Every Day")
        .description("Trigger off at a specific time either on a daily basis or selected days of the week.")
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
            array(DAY_OF_WEEK)
                .label("Day of Week")
                .description("Days at which a workflow will be triggered.")
                .items(integer())
                .options(ScheduleUtils.getDayOfWeekOptions())
                .required(true),
            string(TIMEZONE)
                .label("Timezone")
                .description("The timezone at which the cron expression will be scheduled.")
                .options(ScheduleUtils.getTimeZoneOptions())
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(FIRE_TIME)
                            .description(
                                "The exact date and time when the trigger was activated, formatted according to the " +
                                    "specified timezone."),
                        dateTime(DATETIME)
                            .description("The date and time when the trigger was activated."),
                        integer(HOUR)
                            .description("The hour of the day (0-23) at which the workflow was set to trigger."),
                        integer(MINUTE)
                            .description("The minute of the hour (0-59) at which the workflow was set to trigger."),
                        array(DAY_OF_WEEK)
                            .description(
                                "The specific days of the week (represented as integers) on which the workflow was " +
                                    "set to trigger.")
                            .items(integer()),
                        string(TIMEZONE)
                            .description(
                                "The timezone used for scheduling the cron expression, ensuring the trigger fires at " +
                                    "the correct local time."))))
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final TriggerScheduler triggerScheduler;

    public ScheduleEveryDayTrigger(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    protected void listenerDisable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        TriggerContext triggerContext) {

        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }

    protected void listenerEnable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        ListenerEmitter listenerEmitter, TriggerContext triggerContext) {

        int minute = inputParameters.getRequiredInteger(MINUTE);
        int hour = inputParameters.getRequiredInteger(HOUR);
        String timezone = inputParameters.getRequiredString(TIMEZONE);
        List<Integer> daysOfWeek = inputParameters.getRequiredList(DAY_OF_WEEK, Integer.class);

        triggerScheduler.scheduleScheduleTrigger(
            "0 %s %s ? * %s".formatted(minute, hour, getDayOfWeek(daysOfWeek)),
            timezone,
            Map.of(HOUR, hour, MINUTE, minute, DAY_OF_WEEK, daysOfWeek, TIMEZONE, timezone),
            WorkflowExecutionId.parse(workflowExecutionId));
    }

    private static String getDayOfWeek(List<Integer> daysOfWeek) {
        return daysOfWeek
            .stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }
}


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

import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.util.MapValueUtils;
import com.bytechef.hermes.scheduler.TriggerScheduler;

import java.util.Map;
import java.util.stream.Collectors;

import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DAY_OF_WEEK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.HOUR;
import static com.bytechef.component.schedule.constant.ScheduleConstants.MINUTE;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class ScheduleEveryDayTrigger {

    public final ModifiableTriggerDefinition triggerDefinition = trigger("everyDay")
        .title("Every day")
        .description(
            "Trigger off at a specific time either on a daily basis or selected days of the week.")
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
                .options(ScheduleUtils.getTimeZoneOptions()))
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

    private final TriggerScheduler triggerScheduler;

    public ScheduleEveryDayTrigger(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    protected void listenerEnable(
        Connection connection, Map<String, ?> inputParameters, String workflowExecutionId) {

        triggerScheduler.scheduleScheduleTrigger(
            "0 %s %s ? * %s".formatted(
                MapValueUtils.getInteger(inputParameters, MINUTE), MapValueUtils.getInteger(inputParameters, HOUR),
                getDayOfWeek(inputParameters)),
            MapValueUtils.getString(inputParameters, TIMEZONE), Map.of(
                HOUR, MapValueUtils.getInteger(inputParameters, HOUR),
                MINUTE, MapValueUtils.getInteger(inputParameters, MINUTE),
                DAY_OF_WEEK, MapValueUtils.getMap(inputParameters, DAY_OF_WEEK),
                TIMEZONE, MapValueUtils.getString(inputParameters, TIMEZONE)),
            workflowExecutionId);
    }

    private static String getDayOfWeek(Map<String, ?> inputParameters) {
        return MapValueUtils.getMap(inputParameters, DAY_OF_WEEK)
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() == null || !((Boolean) entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.joining(","));
    }

    protected void listenerDisable(
        Connection connection, Map<String, ?> inputParameters, String workflowExecutionId) {

        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }
}

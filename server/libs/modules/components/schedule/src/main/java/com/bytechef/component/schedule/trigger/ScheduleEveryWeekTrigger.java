/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DAY_OF_WEEK;
import static com.bytechef.component.schedule.constant.ScheduleConstants.HOUR;
import static com.bytechef.component.schedule.constant.ScheduleConstants.MINUTE;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScheduleEveryWeekTrigger {

    public final ModifiableTriggerDefinition triggerDefinition = trigger("everyWeek")
        .title("Every week")
        .description(
            "Trigger off at a specific day of the week.")
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
            integer(DAY_OF_WEEK)
                .label("Day of week")
                .description("Days at which a workflow will be triggered.")
                .options(
                    option("Monday", 1),
                    option("Tuesday", 2),
                    option("Wednesday", 3),
                    option("Thursday", 4),
                    option("Friday", 5),
                    option("Saturday", 6),
                    option("Sunday", 7))
                .required(true),
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
                    integer(DAY_OF_WEEK),
                    string(TIMEZONE)))
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final TriggerScheduler triggerScheduler;

    public ScheduleEveryWeekTrigger(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    protected void listenerDisable(
        ParameterMap inputParameters, ParameterMap connectionParameters, String workflowExecutionId,
        TriggerContext context) {

        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }

    protected void listenerEnable(
        ParameterMap inputParameters, ParameterMap connectionParameters, String workflowExecutionId,
        ListenerEmitter listenerEmitter, TriggerContext context) {

        triggerScheduler.scheduleScheduleTrigger(
            "0 %s %s ? * %s".formatted(
                inputParameters.getInteger(MINUTE), inputParameters.getInteger(HOUR),
                inputParameters.getInteger(DAY_OF_WEEK)),
            inputParameters.getString(TIMEZONE),
            Map.of(
                HOUR, inputParameters.getInteger(HOUR),
                MINUTE, inputParameters.getInteger(MINUTE),
                DAY_OF_WEEK, inputParameters.getInteger(DAY_OF_WEEK),
                TIMEZONE, inputParameters.getString(TIMEZONE)),
            WorkflowExecutionId.parse(workflowExecutionId));
    }
}

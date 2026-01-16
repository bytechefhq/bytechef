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

import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.FIRE_TIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.INTERVAL;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIME_UNIT;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.ZoneId;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScheduleIntervalTrigger {
    public final ModifiableTriggerDefinition triggerDefinition = trigger("interval")
        .title("Interval")
        .description("Trigger off periodically, for example every minute or day, based on a set interval.")
        .type(TriggerType.LISTENER)
        .properties(
            integer(INTERVAL)
                .label("Interval")
                .description(
                    "Specifies the frequency at which the workflow is triggered, based on the selected time unit. " +
                        "For example, an interval of 5 with a time unit of 'Minute' triggers the workflow every " +
                        "5 minutes.")
                .required(true)
                .minValue(1),
            integer(TIME_UNIT)
                .label("Time Unit")
                .description(
                    "Specifies the unit of time used in conjunction with the interval to determine the frequency " +
                        "of workflow triggers.")
                .options(
                    option("Minute", 1),
                    option("Hour", 2),
                    option("Day", 3),
                    option("Month", 4))
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
                        integer(INTERVAL)
                            .description(
                                "The interval value that determines how frequently the workflow is triggered, " +
                                    "based on the selected time unit."),
                        integer(TIME_UNIT)
                            .description(
                                "The unit of time (e.g., minute, hour, day, month) used in conjunction with the " +
                                    "interval to schedule the trigger."))))
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final TriggerScheduler triggerScheduler;

    public ScheduleIntervalTrigger(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    protected void listenerDisable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        TriggerContext contriggerContextext) {

        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }

    protected void listenerEnable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        ListenerEmitter listenerEmitter, TriggerContext triggerContext) {

        int interval = inputParameters.getRequiredInteger(INTERVAL);
        int timeUnit = inputParameters.getRequiredInteger(TIME_UNIT);
        ZoneId zoneId = ZoneId.systemDefault();

        triggerScheduler.scheduleScheduleTrigger(
            switch (timeUnit) {
                case 1 -> "0 */%s * ? * *".formatted(interval);
                case 2 -> "0 0 */%s ? * *".formatted(interval);
                case 3 -> "0 0 0 */%s * ?".formatted(interval);
                case 4 -> "0 0 0 1 */%s ?".formatted(interval);
                default -> throw new IllegalArgumentException("Unexpected time unit value.");
            },
            zoneId.getId(),
            Map.of(INTERVAL, interval, TIME_UNIT, timeUnit),
            WorkflowExecutionId.parse(workflowExecutionId));
    }
}

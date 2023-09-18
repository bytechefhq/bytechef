
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

import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.scheduler.RemoteTriggerScheduler;

import java.time.ZoneId;
import java.util.Map;

import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.INTERVAL;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIME_UNIT;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class ScheduleIntervalTrigger {
    public final ModifiableTriggerDefinition triggerDefinition = trigger("interval")
        .title("Interval")
        .description(
            "Trigger off periodically, for example every minute or day, based on a set interval.")
        .type(TriggerType.LISTENER)
        .properties(
            integer(INTERVAL)
                .label("Interval")
                .description("The hour at which a workflow will be triggered.")
                .required(true)
                .minValue(1),
            integer(TIME_UNIT)
                .label("Day of week")
                .description("Days at which a workflow will be triggered.")
                .options(
                    option("Minute", 1),
                    option("Hour", 2),
                    option("Day", 3),
                    option("Month", 4))
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(DATETIME),
                    integer(INTERVAL),
                    integer(TIME_UNIT)))
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final RemoteTriggerScheduler triggerScheduler;

    public ScheduleIntervalTrigger(RemoteTriggerScheduler triggerScheduler) {
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

        int interval = inputParameters.getInteger(INTERVAL);
        ZoneId zoneId = ZoneId.systemDefault();

        triggerScheduler.scheduleScheduleTrigger(
            switch (inputParameters.getInteger(TIME_UNIT)) {
                case 1 -> "0 */%s * ? * *".formatted(interval);
                case 2 -> "0 0 */%s ? * *".formatted(interval);
                case 3 -> "0 0 0 */%s * ?".formatted(interval);
                case 4 -> "0 0 0 1 */%s ?".formatted(interval);
                default -> throw new ComponentExecutionException("Unexpected time unit value.");
            }, zoneId.getId(), Map.of(
                INTERVAL, inputParameters.getInteger(INTERVAL),
                TIME_UNIT, inputParameters.getInteger(TIME_UNIT)),
            WorkflowExecutionId.parse(workflowExecutionId));

    }
}

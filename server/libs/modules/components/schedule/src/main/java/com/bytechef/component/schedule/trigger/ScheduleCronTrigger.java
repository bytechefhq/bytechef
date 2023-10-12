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
import static com.bytechef.component.schedule.constant.ScheduleConstants.EXPRESSION;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.hermes.scheduler.TriggerScheduler;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScheduleCronTrigger {

    public final ModifiableTriggerDefinition triggerDefinition = trigger("cron")
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
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final TriggerScheduler triggerScheduler;

    public ScheduleCronTrigger(TriggerScheduler triggerScheduler) {
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
            "0 " + inputParameters.getString(EXPRESSION), inputParameters.getString(TIMEZONE),
            Map.of(
                EXPRESSION, inputParameters.getString(EXPRESSION),
                TIMEZONE, inputParameters.getString(TIMEZONE)),
            WorkflowExecutionId.parse(workflowExecutionId));
    }
}

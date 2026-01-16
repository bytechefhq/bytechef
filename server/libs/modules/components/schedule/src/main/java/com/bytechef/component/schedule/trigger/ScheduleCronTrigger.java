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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.EXPRESSION;
import static com.bytechef.component.schedule.constant.ScheduleConstants.FIRE_TIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.TIMEZONE;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.schedule.util.ScheduleUtils;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
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
                        string(EXPRESSION)
                            .description(
                                "The cron schedule expression that defines the timing pattern for triggering " +
                                    "the workflow."),
                        string(TIMEZONE)
                            .description(
                                "The timezone used for scheduling the cron expression, ensuring the trigger fires " +
                                    "at the correct local time."))))
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final TriggerScheduler triggerScheduler;

    public ScheduleCronTrigger(TriggerScheduler triggerScheduler) {
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

        String expression = inputParameters.getRequiredString(EXPRESSION);
        String timezone = inputParameters.getRequiredString(TIMEZONE);

        triggerScheduler.scheduleScheduleTrigger(
            "0 " + expression,
            timezone,
            Map.of(EXPRESSION, expression, TIMEZONE, timezone),
            WorkflowExecutionId.parse(workflowExecutionId));
    }
}

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

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DATETIME;
import static com.bytechef.component.schedule.constant.ScheduleConstants.DAY_OF_WEEK;
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
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ScheduleEveryWeekTrigger {

    public final ModifiableTriggerDefinition triggerDefinition = trigger("everyWeek")
        .title("Every Week")
        .description("Trigger off at a specific day of the week.")
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
            integer(DAY_OF_WEEK)
                .label("Day of Week")
                .description("Days at which a workflow will be triggered.")
                .options(ScheduleUtils.getDayOfWeekOptions())
                .required(true),
            string(TIMEZONE)
                .label("Timezone")
                .description("The timezone at which the cron expression will be scheduled.")
                .options(ScheduleUtils.getTimeZoneOptions()))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(DATETIME),
                        integer(HOUR),
                        integer(MINUTE),
                        integer(DAY_OF_WEEK),
                        string(TIMEZONE))))
        .listenerDisable(this::listenerDisable)
        .listenerEnable(this::listenerEnable);

    private final TriggerScheduler triggerScheduler;

    public ScheduleEveryWeekTrigger(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    protected void listenerDisable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        TriggerContext context) {

        triggerScheduler.cancelScheduleTrigger(workflowExecutionId);
    }

    protected void listenerEnable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        ListenerEmitter listenerEmitter, TriggerContext context) {

        int dayOfWeek = inputParameters.getRequiredInteger(DAY_OF_WEEK);
        int minute = inputParameters.getRequiredInteger(MINUTE);
        int hour = inputParameters.getRequiredInteger(HOUR);
        String timezone = inputParameters.getString(TIMEZONE);

        triggerScheduler.scheduleScheduleTrigger(
            "0 %s %s ? * %s".formatted(minute, hour, dayOfWeek),
            timezone,
            Map.of(HOUR, hour, MINUTE, minute, DAY_OF_WEEK, dayOfWeek, TIMEZONE, timezone),
            WorkflowExecutionId.parse(workflowExecutionId));
    }
}

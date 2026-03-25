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

package com.bytechef.component.wait.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.wait.constant.WaitConstants.AMOUNT;
import static com.bytechef.component.wait.constant.WaitConstants.UNIT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WaitAfterTimeIntervalAction {

    private static final String EXPIRES_AT = "expiresAt";
    private static final String MINUTES = "MINUTES";
    private static final String SCHEDULED_AT = "scheduledAt";

    public static ModifiableActionDefinition of() {
        WaitAfterTimeIntervalAction waitAfterTimeIntervalAction = new WaitAfterTimeIntervalAction();

        return action("afterTimeInterval")
            .title("After Time Interval")
            .description("Pauses the workflow execution for a specified amount of time.")
            .properties(
                integer(AMOUNT)
                    .label("Amount")
                    .description("The amount of time to wait.")
                    .required(true)
                    .defaultValue(1),
                string(UNIT)
                    .label("Unit")
                    .description("The unit of time.")
                    .required(true)
                    .defaultValue(MINUTES)
                    .options(
                        option("Seconds", "SECONDS"),
                        option("Minutes", MINUTES),
                        option("Hours", "HOURS"),
                        option("Days", "DAYS")))
            .output(
                outputSchema(
                    object()
                        .properties(
                            object(ResumeResponse.DATA)
                                .properties(
                                    dateTime(SCHEDULED_AT)
                                        .description(
                                            "The date and time at which the workflow was scheduled to resume."),
                                    integer(AMOUNT)
                                        .description("The amount of time that was waited."),
                                    string(UNIT)
                                        .description("The unit of time that was waited.")),
                            bool(ResumeResponse.RESUMED)
                                .description("Whether the workflow was resumed by a webhook call."))))
            .output(waitAfterTimeIntervalAction::output)
            .perform(waitAfterTimeIntervalAction::perform)
            .resumePerform(waitAfterTimeIntervalAction::resumePerform);
    }

    protected OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OutputResponse.of(
            ResumeResponse.of(
                Map.of(
                    SCHEDULED_AT, LocalDateTime.now(),
                    AMOUNT, inputParameters.getInteger(AMOUNT, 1),
                    UNIT, inputParameters.getString(UNIT, MINUTES))));
    }

    protected Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        int amount = inputParameters.getRequiredInteger(AMOUNT);
        String unit = inputParameters.getRequiredString(UNIT);

        Instant now = Instant.now();

        Instant expiresAt = now.plus(amount, ChronoUnit.valueOf(unit));

        context.suspend(new Suspend(Map.of(EXPIRES_AT, expiresAt.toEpochMilli()), expiresAt));

        return null;
    }

    protected ResumeResponse resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters, Parameters data,
        ActionContext context) {

        long expiresAtMillis = continueParameters.getLong(EXPIRES_AT, System.currentTimeMillis());

        LocalDateTime scheduledAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMillis), ZoneOffset.UTC);

        return ResumeResponse.of(
            Map.of(
                SCHEDULED_AT, scheduledAt,
                AMOUNT, inputParameters.getRequiredInteger(AMOUNT),
                UNIT, inputParameters.getRequiredString(UNIT)));
    }
}

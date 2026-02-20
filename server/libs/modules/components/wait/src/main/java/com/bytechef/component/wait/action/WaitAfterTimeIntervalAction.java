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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.wait.constant.WaitConstants.AMOUNT;
import static com.bytechef.component.wait.constant.WaitConstants.UNIT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WaitAfterTimeIntervalAction {

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
                    .defaultValue("MINUTES")
                    .options(
                        option("Seconds", "SECONDS"),
                        option("Minutes", "MINUTES"),
                        option("Hours", "HOURS"),
                        option("Days", "DAYS")))
            .suspendPerform(waitAfterTimeIntervalAction::suspendPerform)
            .resumePerform(waitAfterTimeIntervalAction::resumePerform);
    }

    protected Suspend suspendPerform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        int amount = inputParameters.getRequiredInteger(AMOUNT);
        String unit = inputParameters.getRequiredString(UNIT);

        ChronoUnit chronoUnit = ChronoUnit.valueOf(unit);

        Instant expiresAt = Instant.now()
            .plus(amount, chronoUnit);

        return new Suspend(
            Map.of("expiresAt", expiresAt.toEpochMilli(), "amount", amount, "unit", unit), expiresAt);
    }

    protected Object resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters,
        ActionContext context) {

        long expiresAtMillis = continueParameters.getLong("expiresAt");

        Instant scheduledAt = Instant.ofEpochMilli(expiresAtMillis);

        int amount = continueParameters.getInteger("amount");
        String unit = continueParameters.getString("unit");

        return Map.of("scheduledAt", scheduledAt, "amount", amount, "unit", unit);
    }
}

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

package com.bytechef.component.delay.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.delay.constant.DelayConstants.MILLIS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DelaySleepAction {

    public static ModifiableActionDefinition of() {
        DelaySleepAction delaySleepAction = new DelaySleepAction();

        return action("sleep")
            .title("Sleep")
            .description("Delay action execution.")
            .properties(
                integer(MILLIS)
                    .label("Millis")
                    .description("Time in milliseconds.")
                    .required(true)
                    .defaultValue(1))
            .suspendPerform(delaySleepAction::suspendPerform)
            .resumePerform(delaySleepAction::resumePerform);
    }

    protected Suspend suspendPerform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        long millis = inputParameters.containsKey(MILLIS) ? inputParameters.getLong(MILLIS) : 1000;

        Instant expiresAt = Instant.now()
            .plusMillis(millis);

        return new Suspend(Map.of("expiresAt", expiresAt.toEpochMilli(), "delayMillis", millis), expiresAt);
    }

    protected Object resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters,
        ActionContext context) {

        long executeAtMillis = continueParameters.getLong("expiresAt");

        Instant executeAt = Instant.ofEpochMilli(executeAtMillis);

        long millis = continueParameters.getLong("delayMillis");

        return Map.of("scheduledAt", executeAt, "delayMillis", millis);
    }
}

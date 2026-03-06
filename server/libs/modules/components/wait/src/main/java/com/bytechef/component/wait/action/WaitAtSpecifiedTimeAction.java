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
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.wait.constant.WaitConstants.DATE_TIME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WaitAtSpecifiedTimeAction {

    public static ModifiableActionDefinition of() {
        WaitAtSpecifiedTimeAction waitAtSpecifiedTimeAction = new WaitAtSpecifiedTimeAction();

        return action("atSpecifiedTime")
            .title("At Specified Time")
            .description("Pauses the workflow execution until a specified date and time.")
            .properties(
                dateTime(DATE_TIME)
                    .label("Date and Time")
                    .description("The date and time to wait until before resuming workflow execution.")
                    .required(true))
            .suspendPerform(waitAtSpecifiedTimeAction::suspendPerform)
            .resumePerform(waitAtSpecifiedTimeAction::resumePerform);
    }

    protected Suspend suspendPerform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        LocalDateTime localDateTime = inputParameters.getRequiredLocalDateTime(DATE_TIME);

        Instant expiresAt = localDateTime.atZone(ZoneId.systemDefault())
            .toInstant();

        return new Suspend(Map.of("expiresAt", expiresAt.toEpochMilli()), expiresAt);
    }

    protected Object resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters,
        ActionContext context) {

        long expiresAtMillis = continueParameters.getLong("expiresAt");

        Instant scheduledAt = Instant.ofEpochMilli(expiresAtMillis);

        return Map.of("scheduledAt", scheduledAt);
    }
}

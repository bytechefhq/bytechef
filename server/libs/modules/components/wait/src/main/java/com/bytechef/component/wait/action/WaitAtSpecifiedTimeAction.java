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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.wait.constant.WaitConstants.DATE_TIME;
import static com.bytechef.component.wait.constant.WaitConstants.TIMEZONE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.wait.util.WaitUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
                object(ResumeResponse.DATA)
                    .properties(
                        dateTime(DATE_TIME)
                            .label("Date and Time")
                            .description("The date and time to wait until before resuming workflow execution.")
                            .required(true),
                        string(TIMEZONE)
                            .label("Timezone")
                            .description("The timezone in which the specified date and time should be interpreted.")
                            .options(WaitUtils.getTimeZoneOptions())
                            .required(true)),
                bool(ResumeResponse.RESUMED)
                    .description("Whether the workflow was resumed by a webhook call."))
            .output(waitAtSpecifiedTimeAction::output)
            .perform(waitAtSpecifiedTimeAction::perform)
            .resumePerform(waitAtSpecifiedTimeAction::resumePerform);
    }

    protected OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        long expiresAtMillis = inputParameters.getLong("expiresAt", System.currentTimeMillis());
        String timezone = inputParameters.getString("timezone", "UTC");

        LocalDateTime scheduledAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMillis), ZoneId.of(timezone));

        return OutputResponse.of(ResumeResponse.of(Map.of("scheduledAt", scheduledAt)));
    }

    protected Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        LocalDateTime localDateTime = inputParameters.getRequiredLocalDateTime(DATE_TIME);
        String timezone = inputParameters.getRequiredString(TIMEZONE);

        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timezone));

        Instant expiresAt = zonedDateTime.toInstant();

        context.suspend(new Suspend(Map.of("expiresAt", expiresAt.toEpochMilli(), "timezone", timezone), expiresAt));

        return null;
    }

    protected ResumeResponse resumePerform(
        Parameters inputParameters, Parameters connectionParameters, Parameters continueParameters, Parameters data,
        ActionContext context) {

        long expiresAtMillis = continueParameters.getLong("expiresAt", System.currentTimeMillis());
        String timezone = continueParameters.getString("timezone", "UTC");

        LocalDateTime scheduledAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMillis), ZoneId.of(timezone));

        return ResumeResponse.of(Map.of("scheduledAt", scheduledAt));
    }
}

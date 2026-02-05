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

package com.bytechef.component.date.helper.action;

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE;
import static com.bytechef.component.date.helper.util.DateHelperUtils.formatDuration;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author Nikolina Spehar
 */
public class DateHelperGetTimeFromNowAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getTimeFromNow")
        .title("Get Time From Now")
        .description(
            "Returns a human readable date relative to the current time, such as “in 2 months”, or “14 days ago”")
        .properties(
            dateTime(DATE)
                .label("Date")
                .description("Date to which time will be calculated.")
                .required(true))
        .output(outputSchema(string().description("Human readable time to inputted date.")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#get-time-from-now")
        .perform(DateHelperGetTimeFromNowAction::perform);

    private DateHelperGetTimeFromNowAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        LocalDateTime date = inputParameters.getRequiredLocalDateTime(DATE);
        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(now, date);
        long totalSeconds = duration.getSeconds();

        String formatedDuration = formatDuration(totalSeconds);

        return totalSeconds < 0 ? formatedDuration + " ago" : "in " + formatedDuration;
    }
}

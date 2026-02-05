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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT_PROPERTY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INPUT_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getFormattedDate;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;

/**
 * @author Monika Kušter
 */
public class DateHelperAddTimeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addTime")
        .title("Add Time")
        .description("Add time to the date.")
        .properties(
            dateTime(INPUT_DATE)
                .label("Date")
                .required(true),
            DATE_FORMAT_PROPERTY,
            integer(YEAR)
                .label("Year")
                .description("Years to add.")
                .required(false),
            integer(MONTH)
                .label("Month")
                .description("Months to add.")
                .required(false),
            integer(DAY)
                .label("Day")
                .description("Days to add.")
                .required(false),
            integer(HOUR)
                .label("Hour")
                .description("Hours to add.")
                .required(false),
            integer(MINUTE)
                .label("Minute")
                .description("Minutes to add.")
                .required(false),
            integer(SECOND)
                .label("Second")
                .description("Seconds to add.")
                .required(false))
        .output(outputSchema(string().description("Date with added time.")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#add-time")
        .perform(DateHelperAddTimeAction::perform);

    private DateHelperAddTimeAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        LocalDateTime inputDate = inputParameters.getRequiredLocalDateTime(INPUT_DATE);

        inputDate = inputDate.plusYears(inputParameters.getInteger(YEAR, 0))
            .plusMonths(inputParameters.getInteger(MONTH, 0))
            .plusDays(inputParameters.getInteger(DAY, 0))
            .plusHours(inputParameters.getInteger(HOUR, 0))
            .plusMinutes(inputParameters.getInteger(MINUTE, 0))
            .plusSeconds(inputParameters.getInteger(SECOND, 0));

        return getFormattedDate(inputParameters.getRequiredString(DATE_FORMAT), inputDate);
    }
}

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

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;

/**
 * @author Monika Kušter
 */
public class DateHelperSubtractTimeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("subtractTime")
        .title("Subtract Time")
        .description("Subtract time from date")
        .properties(
            dateTime(INPUT_DATE)
                .label("Date")
                .required(true),
            DATE_FORMAT_PROPERTY,
            integer(YEAR)
                .label("Year")
                .description("Years to subtract.")
                .required(false),
            integer(MONTH)
                .label("Month")
                .description("Months to subtract.")
                .required(false),
            integer(DAY)
                .label("Day")
                .description("Days to subtract.")
                .required(false),
            integer(HOUR)
                .label("Hour")
                .description("Hours to subtract.")
                .required(false),
            integer(MINUTE)
                .label("Minute")
                .description("Minutes to subtract.")
                .required(false),
            integer(SECOND)
                .label("Second")
                .description("Seconds to subtract.")
                .required(false))
        .output(outputSchema(dateTime().description("Date with subtracted time.")))
        .perform(DateHelperSubtractTimeAction::perform);

    private DateHelperSubtractTimeAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        LocalDateTime inputDate = inputParameters.getRequiredLocalDateTime(INPUT_DATE);

        inputDate = inputDate
            .minusYears(inputParameters.getInteger(YEAR, 0))
            .minusMonths(inputParameters.getInteger(MONTH, 0))
            .minusDays(inputParameters.getInteger(DAY, 0))
            .minusHours(inputParameters.getInteger(HOUR, 0))
            .minusMinutes(inputParameters.getInteger(MINUTE, 0))
            .minusSeconds(inputParameters.getInteger(SECOND, 0));

        return getFormattedDate(inputParameters.getRequiredString(DATE_FORMAT), inputDate);
    }
}

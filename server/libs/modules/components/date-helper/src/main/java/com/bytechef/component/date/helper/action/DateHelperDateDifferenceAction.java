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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author Monika Kušter
 */
public class DateHelperDateDifferenceAction {

    protected static final String START_DATE = "startDate";
    protected static final String END_DATE = "endDate";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("dateDifference")
        .title("Date Difference")
        .description("Get the difference between two dates.")
        .properties(
            dateTime(START_DATE)
                .label("Start Date")
                .required(true),
            dateTime(END_DATE)
                .label("End Date")
                .required(true),
            string(UNIT)
                .label("Unit")
                .description("The unit of difference between the two dates.")
                .options(
                    option("Year", YEAR),
                    option("Month", MONTH),
                    option("Day", DAY),
                    option("Hour", HOUR),
                    option("Minute", MINUTE),
                    option("Second", SECOND))
                .required(true))
        .output(outputSchema(number().description("Difference between two dates.")))
        .perform(DateHelperDateDifferenceAction::perform);

    private DateHelperDateDifferenceAction() {
    }

    protected static Long perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String unitDifference = inputParameters.getRequiredString(UNIT);

        ChronoUnit chronoUnit = switch (unitDifference) {
            case YEAR -> ChronoUnit.YEARS;
            case MONTH -> ChronoUnit.MONTHS;
            case DAY -> ChronoUnit.DAYS;
            case HOUR -> ChronoUnit.HOURS;
            case MINUTE -> ChronoUnit.MINUTES;
            case SECOND -> ChronoUnit.SECONDS;
            default -> throw new IllegalArgumentException("Unsupported unit " + unitDifference);
        };

        LocalDateTime startDate = inputParameters.getRequiredLocalDateTime(START_DATE);
        LocalDateTime endDate = inputParameters.getRequiredLocalDateTime(END_DATE);

        return startDate.until(endDate, chronoUnit);
    }
}

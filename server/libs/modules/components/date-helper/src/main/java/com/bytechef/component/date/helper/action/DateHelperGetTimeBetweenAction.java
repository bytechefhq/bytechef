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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class DateHelperGetTimeBetweenAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getTimeBetween")
        .title("Get Time Between")
        .description(
            "Get the time between two dates, as hh:mm:ss. If the second date is before the first date, the result " +
                "will be negative.")
        .properties(
            dateTime(DATE_A)
                .label("Start Date")
                .description("Start date of the interval.")
                .required(true),
            dateTime(DATE_B)
                .label("End Date")
                .description("End date of the interval.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .description("Time between two dates.")
                    .properties(
                        integer(YEAR)
                            .description("Number of years in between dates."),
                        integer(MONTH)
                            .description("Number of months in between dates."),
                        integer(DAY)
                            .description("Number of days in between dates."),
                        integer(HOUR)
                            .description("Number of hours in between dates."),
                        integer(MINUTE)
                            .description("Number of minutes in between dates."),
                        integer(SECOND)
                            .description("Number of seconds in between dates."))))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#get-time-between")
        .perform(DateHelperGetTimeBetweenAction::perform);

    private DateHelperGetTimeBetweenAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        LocalDateTime dateA = inputParameters.getRequiredLocalDateTime(DATE_A);
        LocalDateTime dateB = inputParameters.getRequiredLocalDateTime(DATE_B);

        Period period = Period.between(dateA.toLocalDate(), dateB.toLocalDate());
        Duration duration = Duration.between(dateA, dateB);

        Map<String, Object> result = new LinkedHashMap<>();

        result.put(YEAR, period.getYears());
        result.put(MONTH, period.getMonths());
        result.put(DAY, period.getDays());
        result.put(HOUR, duration.toHoursPart());
        result.put(MINUTE, duration.toMinutesPart());
        result.put(SECOND, duration.toSecondsPart());

        return result;
    }
}

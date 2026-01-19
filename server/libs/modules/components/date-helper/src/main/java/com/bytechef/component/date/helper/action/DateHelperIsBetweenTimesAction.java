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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INCLUSIVE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INCLUSIVE_SECONDS;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_B;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.date.helper.constants.DateHelperComparisonEnum;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * @author Nikolina Spehar
 */
public class DateHelperIsBetweenTimesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("IsBetweenTimes")
        .title("Is Between Times?")
        .description("Check to see whether a date falls within a time range.")
        .properties(
            dateTime(DATE)
                .label("Date")
                .description("Date to check if it is in the range.")
                .required(true),
            time(TIME_A)
                .label("Start Time")
                .description("Start time of the interval.")
                .required(true),
            time(TIME_B)
                .label("End Time")
                .description("End time of the interval.")
                .required(true),
            bool(INCLUSIVE)
                .label("Inclusive")
                .description("Whether the boundary is inclusive or not.")
                .defaultValue(false),
            bool(INCLUSIVE_SECONDS)
                .label("Inclusive Seconds")
                .description("Whether the seconds will be compared.")
                .defaultValue(true))
        .output(outputSchema(bool().description("Whether the date falls within given date range.")))
        .perform(DateHelperIsBetweenTimesAction::perform);

    private DateHelperIsBetweenTimesAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        LocalDateTime localDateTime = inputParameters.getRequiredLocalDateTime(DATE);

        LocalDateTime startLocalDateTime = getLocalDateTime(
            inputParameters.getRequiredLocalTime(TIME_A), localDateTime);

        LocalDateTime endLocalDateTime = getLocalDateTime(inputParameters.getRequiredLocalTime(TIME_B), localDateTime);

        boolean inclusive = inputParameters.getBoolean(INCLUSIVE);
        boolean inclusiveSeconds = inputParameters.getBoolean(INCLUSIVE_SECONDS);

        localDateTime = includeSeconds(inclusiveSeconds, localDateTime);
        startLocalDateTime = includeSeconds(inclusiveSeconds, startLocalDateTime);
        endLocalDateTime = includeSeconds(inclusiveSeconds, endLocalDateTime);

        return isInTimeRange(startLocalDateTime, endLocalDateTime, localDateTime, inclusive);
    }

    private static LocalDateTime getLocalDateTime(LocalTime localTime, LocalDateTime localDateTime) {
        return localTime.atDate(localDateTime.toLocalDate());
    }

    private static LocalDateTime includeSeconds(boolean includeSeconds, LocalDateTime localDateTime) {
        return includeSeconds ? localDateTime : localDateTime.truncatedTo(ChronoUnit.MINUTES);
    }

    private static boolean isInTimeRange(
        LocalDateTime start, LocalDateTime end, LocalDateTime date, boolean inclusive) {

        boolean crossesMidnight = end.isBefore(start);

        DateHelperComparisonEnum afterStart =
            inclusive ? DateHelperComparisonEnum.IS_AFTER_OR_EQUAL : DateHelperComparisonEnum.IS_AFTER;
        DateHelperComparisonEnum beforeEnd =
            inclusive ? DateHelperComparisonEnum.IS_BEFORE_OR_EQUAL : DateHelperComparisonEnum.IS_BEFORE;

        if (crossesMidnight) {
            return afterStart.compare(date, start) || beforeEnd.compare(date, end);
        } else {
            return afterStart.compare(date, start) && beforeEnd.compare(date, end);
        }
    }
}

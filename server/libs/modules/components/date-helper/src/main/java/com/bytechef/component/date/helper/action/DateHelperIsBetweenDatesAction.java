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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INCLUSIVE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.RESOLUTION;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.RESOLUTION_PROPERTY;
import static com.bytechef.component.date.helper.util.DateHelperUtils.applyResolution;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.component.date.helper.constants.DateHelperComparisonEnum;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;

/**
 * @author Nikolina Spehar
 */
public class DateHelperIsBetweenDatesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("IsBetweenDates")
        .title("Is Between Dates?")
        .description("Check to see whether a date falls within a date range.")
        .properties(
            dateTime(DATE)
                .label("Date")
                .description("Date to check if it is in the range.")
                .required(true),
            dateTime(DATE_A)
                .label("Start Date")
                .description("Start date of the interval.")
                .required(true),
            dateTime(DATE_B)
                .label("End Date")
                .description("End date of the interval.")
                .required(true),
            RESOLUTION_PROPERTY,
            bool(INCLUSIVE)
                .label("Inclusive")
                .description("Whether the boundary is inclusive or not.")
                .defaultValue(false))
        .output(outputSchema(bool().description("Whether the date falls within given date range.")))
        .perform(DateHelperIsBetweenDatesAction::perform);

    private DateHelperIsBetweenDatesAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String resolution = inputParameters.getRequiredString(RESOLUTION);

        LocalDateTime date = applyResolution(resolution, inputParameters.getRequiredLocalDateTime(DATE));
        LocalDateTime startDate = applyResolution(resolution, inputParameters.getRequiredLocalDateTime(DATE_A));
        LocalDateTime endDate = applyResolution(resolution, inputParameters.getRequiredLocalDateTime(DATE_B));

        boolean inclusive = inputParameters.getBoolean(INCLUSIVE);

        DateHelperComparisonEnum startDateComparisonEnum =
            inclusive ? DateHelperComparisonEnum.IS_AFTER_OR_EQUAL : DateHelperComparisonEnum.IS_AFTER;

        DateHelperComparisonEnum endDateComparisonEnum =
            inclusive ? DateHelperComparisonEnum.IS_BEFORE_OR_EQUAL : DateHelperComparisonEnum.IS_BEFORE;

        return startDateComparisonEnum.compare(date, startDate) && endDateComparisonEnum.compare(date, endDate);
    }
}

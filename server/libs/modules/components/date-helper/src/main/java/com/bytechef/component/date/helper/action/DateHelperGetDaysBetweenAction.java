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
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * @author Nikolina Spehar
 */
public class DateHelperGetDaysBetweenAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDaysBetween")
        .title("Get Days Between")
        .description(
            "Get the number of days between two dates, rounded to the nearest day. If the second date is before the " +
                "first date, the result will be negative.")
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
                integer()
                    .description(
                        "Number of days between start and end date. If the end date was before start date number " +
                            "will be negative.")))
        .perform(DateHelperGetDaysBetweenAction::perform);

    private DateHelperGetDaysBetweenAction() {
    }

    public static int perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        LocalDateTime dateA = inputParameters.getRequiredLocalDateTime(DATE_A);
        LocalDateTime dateB = inputParameters.getRequiredLocalDateTime(DATE_B);

        Period period = Period.between(dateA.toLocalDate(), dateB.toLocalDate());

        return period.getDays();
    }
}

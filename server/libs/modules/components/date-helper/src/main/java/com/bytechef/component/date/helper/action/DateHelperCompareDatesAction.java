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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.COMPARISON;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_A;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_B;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.RESOLUTION;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.RESOLUTION_PROPERTY;
import static com.bytechef.component.date.helper.util.DateHelperUtils.applyResolution;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.date.helper.constants.DateHelperComparisonEnum;
import com.bytechef.component.date.helper.util.DateHelperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;

/**
 * @author Nikolina Spehar
 */
public class DateHelperCompareDatesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("compareDates")
        .title("Compare Dates")
        .description("Compares two dates.")
        .properties(
            dateTime(DATE_A)
                .label("Date A")
                .description("First date that will be compared.")
                .required(true),
            dateTime(DATE_B)
                .label("Date B")
                .description("Second date that will be compared.")
                .required(true),
            RESOLUTION_PROPERTY,
            string(COMPARISON)
                .label("Comparison")
                .description("The type of comparison to be performed.")
                .options(DateHelperUtils.getComparisonOptions())
                .required(true))
        .output(outputSchema(bool().description("Result of comparison.")))
        .perform(DateHelperCompareDatesAction::perform);

    private DateHelperCompareDatesAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String resolution = inputParameters.getRequiredString(RESOLUTION);

        LocalDateTime dateA = applyResolution(resolution, inputParameters.getRequiredLocalDateTime(DATE_A));
        LocalDateTime dateB = applyResolution(resolution, inputParameters.getRequiredLocalDateTime(DATE_B));

        return DateHelperComparisonEnum.valueOf(inputParameters.getRequiredString(COMPARISON))
            .compare(dateA, dateB);
    }
}

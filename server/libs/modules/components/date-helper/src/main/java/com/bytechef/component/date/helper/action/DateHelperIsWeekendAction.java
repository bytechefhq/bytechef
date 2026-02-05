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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.INPUT_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.date.helper.util.DateHelperUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Monika Kušter
 */
public class DateHelperIsWeekendAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("isWeekend")
        .title("Is Weekend?")
        .description("Check if the current date is a weekend.")
        .properties(
            dateTime(INPUT_DATE)
                .label("Date")
                .description(
                    "Date to check to see if it is a weekend. We only accept yyyy-MM-ddTHH:mm:ss, use our Format " +
                        "Date action to transform the date format")
                .required(true),
            string(TIME_ZONE)
                .label("Time Zone")
                .description("Time zone you are in.")
                .options((OptionsFunction<String>) DateHelperUtils::getZoneOptions)
                .required(true))
        .output(outputSchema(bool().description("True if the date is a weekend, false otherwise.")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#is-weekend")
        .perform(DateHelperIsWeekendAction::perform);

    private DateHelperIsWeekendAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        LocalDateTime localDateTime = inputParameters.getRequiredLocalDateTime(INPUT_DATE);
        String timeZone = inputParameters.getRequiredString(TIME_ZONE);

        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZone));

        DayOfWeek dayOfWeek = zonedDateTime.getDayOfWeek();

        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}

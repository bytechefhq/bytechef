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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_HOURS_END;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_HOURS_START;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_WEEK_END;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.BUSINESS_WEEK_START;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INPUT_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;

import com.bytechef.component.date.helper.util.DateHelperUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @author Monika Kušter
 */
public class DateHelperIsBusinessHoursAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("isBusinessHours")
        .title("Is Business Hours?")
        .description("Check to see if it's business hours or not.")
        .properties(
            dateTime(INPUT_DATE)
                .label("Date")
                .description("Date to check to see if it is business hours.")
                .required(true),
            integer(BUSINESS_WEEK_START)
                .label("Business Week Start")
                .description("First day of the business week.")
                .options(DateHelperUtils.getDayOfWeekOptions())
                .required(true),
            integer(BUSINESS_WEEK_END)
                .label("Business Week End")
                .description("Last day of the business week.")
                .options(DateHelperUtils.getDayOfWeekOptions())
                .required(true),
            time(BUSINESS_HOURS_START)
                .label("Business Hours Start")
                .description("Time of the day that business hours start.")
                .required(true),
            time(BUSINESS_HOURS_END)
                .label("Business Hours End")
                .description("Time of the day that business hours end.")
                .required(true),
            string(TIME_ZONE)
                .label("Time Zone")
                .description("Time zone to check business hours for.")
                .options((OptionsFunction<String>) DateHelperUtils::getZoneOptions)
                .required(true))
        .output(outputSchema(bool().description("True if the date is a weekend, false otherwise.")))
        .perform(DateHelperIsBusinessHoursAction::perform);

    private DateHelperIsBusinessHoursAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        LocalDateTime localDateTime = inputParameters.getRequiredLocalDateTime(INPUT_DATE);
        String timeZone = inputParameters.getRequiredString(TIME_ZONE);

        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZone));

        DayOfWeek dayOfWeek = zonedDateTime.getDayOfWeek();
        LocalTime localTime = zonedDateTime.toLocalTime();

        int businessWeekStart = inputParameters.getRequiredInteger(BUSINESS_WEEK_START);
        int businessWeekEnd = inputParameters.getRequiredInteger(BUSINESS_WEEK_END);
        LocalTime businessHoursStart = inputParameters.getRequiredLocalTime(BUSINESS_HOURS_START);
        LocalTime businessHoursEnd = inputParameters.getRequiredLocalTime(BUSINESS_HOURS_END);

        boolean isWithinBusinessWeek =
            businessWeekStart <= dayOfWeek.getValue() && dayOfWeek.getValue() <= businessWeekEnd;
        boolean isWithinBusinessHours = businessHoursStart.isBefore(localTime) && businessHoursEnd.isAfter(localTime);

        return isWithinBusinessWeek && isWithinBusinessHours;
    }
}

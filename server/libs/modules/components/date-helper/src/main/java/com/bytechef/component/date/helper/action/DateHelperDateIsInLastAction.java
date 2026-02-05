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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.IN_LAST;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT_PROPERTY;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getChronoUnit;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.date.helper.util.DateHelperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * @author Nikolina Spehar
 */
public class DateHelperDateIsInLastAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("dateIsInLast")
        .title("Date Is in the Last")
        .description(
            "Allows you to easily check if a given date has occurred in the last X number of seconds, minutes, " +
                "hours, or days.")
        .properties(
            dateTime(DATE)
                .label("Date")
                .description(
                    "Date for which you want to check is it in the last X number of selected time units. We only " +
                        "accept yyyy-MM-ddTHH:mm:ss, use our Format Date action to transform the date format")
                .required(true),
            string(TIME_ZONE)
                .label("Time Zone")
                .description("Time zone to use when checking the date.")
                .options((OptionsFunction<String>) DateHelperUtils::getZoneOptions)
                .required(true),
            integer(IN_LAST)
                .label("In Last")
                .description("Number of how many time units.")
                .required(true),
            UNIT_PROPERTY)
        .output(outputSchema(bool().description("Whether the date is in the last X number of selected time units.")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#date-is-in-the-last")
        .perform(DateHelperDateIsInLastAction::perform);

    private DateHelperDateIsInLastAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        int inLast = inputParameters.getRequiredInteger(IN_LAST);

        String inLastUnit = inputParameters.getRequiredString(UNIT);

        LocalDateTime localDateTime = inputParameters.getRequiredLocalDateTime(DATE)
            .truncatedTo(ChronoUnit.SECONDS);

        String timeZone = inputParameters.getRequiredString(TIME_ZONE);
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timeZone))
            .truncatedTo(ChronoUnit.SECONDS);

        LocalDateTime threshold = now.minus(inLast, getChronoUnit(inLastUnit))
            .truncatedTo(ChronoUnit.SECONDS);

        return !localDateTime.isBefore(threshold) && !localDateTime.isAfter(now);
    }
}

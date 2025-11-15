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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY_OF_WEEK;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.INPUT_DATE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH_NAME;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * @author Monika Kušter
 */
public class DateHelperExtractDateUnitsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractDateUnits")
        .title("Extract Date Units")
        .description(
            "Extracts specific units (year, month, day, hour, minute, second, day of week, month name, date, or " +
                "time) from a given date.")
        .properties(
            dateTime(INPUT_DATE)
                .label("Date")
                .description("The date from which to extract the specified unit.")
                .required(true),
            string(UNIT)
                .label("Unit to Extract")
                .description("Unit to extract from the input date.")
                .options(
                    option("Year", YEAR),
                    option("Month", MONTH),
                    option("Day", DAY),
                    option("Hour", HOUR),
                    option("Minute", MINUTE),
                    option("Second", SECOND),
                    option("Day of Week", DAY_OF_WEEK),
                    option("Month name", MONTH_NAME),
                    option("Date", DATE),
                    option("Time", TIME))
                .required(true))
        .output()
        .perform(DateHelperExtractDateUnitsAction::perform);

    private DateHelperExtractDateUnitsAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        LocalDateTime inputDate = inputParameters.getRequiredLocalDateTime(INPUT_DATE);

        String unitToExtract = inputParameters.getRequiredString(UNIT);

        return switch (unitToExtract) {
            case YEAR -> inputDate.getYear();
            case MONTH -> inputDate.getMonthValue();
            case DAY -> inputDate.getDayOfMonth();
            case HOUR -> inputDate.getHour();
            case MINUTE -> inputDate.getMinute();
            case SECOND -> inputDate.getSecond();
            case DAY_OF_WEEK -> {
                DayOfWeek dayOfWeek = inputDate.getDayOfWeek();

                yield dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault());
            }
            case MONTH_NAME -> {
                Month month = inputDate.getMonth();

                yield month.getDisplayName(TextStyle.FULL, Locale.getDefault());
            }
            case DATE -> inputDate.toLocalDate();
            case TIME -> inputDate.toLocalTime();
            default -> throw new IllegalArgumentException("Unsupported unit " + unitToExtract);
        };
    }
}

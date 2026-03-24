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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT_PROPERTY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_STRING;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Marko Krišković
 */
public class DateHelperConvertToDateAction {

    protected static final String DATE_TYPE = "date";
    protected static final String DATE_TIME_TYPE = "dateTime";
    protected static final String TYPE = "type";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("convertToDate")
        .title("Convert to Date")
        .description("Converts a date string into a Date or DateTime value.")
        .properties(
            string(DATE_STRING)
                .label("Date String")
                .description("The string to convert to a date.")
                .required(true),
            string("dateFormatType")
                .label("Date Format Type")
                .description("The type of date format to use.")
                .options(
                    option("Standard", "standard"),
                    option("Custom", "custom"))
                .required(true),
            DATE_FORMAT_PROPERTY
                .displayCondition("dateFormatType == 'standard'"),
            string(DATE_FORMAT)
                .label("Date Format")
                .description(
                    "The format pattern of the input string (e.g. yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss). " +
                        "If not provided, ISO format is assumed.")
                .displayCondition("dateFormatType == 'custom'")
                .required(false),
            string(TYPE)
                .label("Output Type")
                .description("The type of date value to produce.")
                .options(
                    option("Date", DATE_TYPE, "Returns a date without time (LocalDate)."),
                    option("Date Time", DATE_TIME_TYPE, "Returns a date with time (LocalDateTime)."))
                .defaultValue(DATE_TIME_TYPE)
                .required(true))
        .output(outputSchema(dateTime().description("Parsed date value.")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#convert-to-date")
        .perform(DateHelperConvertToDateAction::perform);

    private DateHelperConvertToDateAction() {
    }

    protected static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String inputString = inputParameters.getRequiredString(DATE_STRING);
        String format = inputParameters.getString(DATE_FORMAT);
        String type = inputParameters.getRequiredString(TYPE);

        if (DATE_TYPE.equals(type)) {
            if (format != null && !format.isBlank()) {
                return LocalDate.parse(inputString, DateTimeFormatter.ofPattern(format));
            }

            return LocalDate.parse(inputString);
        } else {
            if (format != null && !format.isBlank()) {
                return LocalDateTime.parse(inputString, DateTimeFormatter.ofPattern(format));
            }

            return LocalDateTime.parse(inputString);
        }
    }
}

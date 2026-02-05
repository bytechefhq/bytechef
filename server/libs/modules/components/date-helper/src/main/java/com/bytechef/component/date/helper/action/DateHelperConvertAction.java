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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_TIMESTAMP;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Igor Beslic
 * @author Monika Kušter
 */
public class DateHelperConvertAction {

    protected static final String ISO8601_DATE_FORMAT = "yyyy-MM-dd";
    protected static final String ISO8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("convertUnixTimestampToIso8601")
        .title("Convert Date Timestamp")
        .description("Converts UNIX timestamp to ISO8601 format.")
        .properties(
            number(DATE_TIMESTAMP)
                .label("UNIX Timestamp")
                .description("UNIX Timestamp in seconds (10 digits) or milliseconds (13 digits)")
                .maxNumberPrecision(0)
                .required(true),
            string(DATE_FORMAT)
                .label("Date Format")
                .description("Formatting that should be applied the text representation of date.")
                .controlType(ControlType.SELECT)
                .options(
                    option("ISO8601 Date Time", ISO8601_DATE_TIME_FORMAT,
                        "Get date in yyyy-MM-ddTHH:mm:ssZ"),
                    option("ISO8601 Date", ISO8601_DATE_FORMAT, "Get date in yyyy-MM-dd"))
                .required(true)
                .defaultValue(ISO8601_DATE_TIME_FORMAT))
        .output(outputSchema(string().description("ISO8601 Date")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#convert-date-timestamp")
        .perform(DateHelperConvertAction::perform);

    private DateHelperConvertAction() {
    }

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        long unixTimestamp = inputParameters.getRequiredLong(DATE_TIMESTAMP);

        if (getDigitCount(unixTimestamp) == 10) {
            unixTimestamp *= 1000;
        }

        Date date = new Date(unixTimestamp);

        DateFormat dateFormat = new SimpleDateFormat(inputParameters.getRequiredString(DATE_FORMAT));

        return dateFormat.format(date);
    }

    private static int getDigitCount(long value) {
        String string = String.valueOf(Math.abs(value));

        return string.length();
    }
}

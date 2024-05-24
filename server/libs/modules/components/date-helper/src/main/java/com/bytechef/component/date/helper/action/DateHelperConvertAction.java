/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.date.helper.constants.DateHelperConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Igor Beslic
 */
public class DateHelperConvertAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION =
        action(DateHelperConstants.CONVERT_UNIX_TIMESTAMP_TO_ISO8601)
            .title("Convert Date Timestamp")
            .description("Converts UNIX timestamp to ISO8601 format.")
            .properties(
                string(DateHelperConstants.DATE_TIMESTAMP)
                    .label("UNIX Timestamp.")
                    .description("UNIX Timestamp in seconds (10 digits) or milliseconds (13 digits)")
                    .controlType(Property.ControlType.NUMBER)
                    .required(true),
                string(DateHelperConstants.DATE_FORMAT)
                    .label("Date Format")
                    .description("Formatting that should be applied the text representation of date.")
                    .controlType(Property.ControlType.SELECT)
                    .options(
                        option(DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME,
                            DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE,
                            "Get date in yyyy-MM-ddTHH:mm:ssZ"),
                        option(DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE,
                            DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_VALUE, "Get date in yyyy-MM-dd"))
                    .required(true)
                    .defaultValue(DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE))
            .output()
            .perform(DateHelperConvertAction::perform);

    protected static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        long unixTimestamp = inputParameters.getRequiredLong(DateHelperConstants.DATE_TIMESTAMP);

        if (getDigitCount(unixTimestamp) == 10) {
            unixTimestamp = unixTimestamp * 1000;
        }

        Date date = new Date(unixTimestamp);

        DateFormat dateFormat = getDateFormat(inputParameters.getRequiredString(DateHelperConstants.DATE_FORMAT));

        return dateFormat.format(date);
    }

    private static DateFormat getDateFormat(String format) {
        return switch (format) {
            case DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_VALUE -> new SimpleDateFormat("yyyy-MM-dd");
            case DateHelperConstants.DATE_FORMAT_OPTION_ISO8601_DATE_TIME_VALUE ->
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            default -> throw new IllegalArgumentException("Unsupported format " + format);
        };

    }

    private static int getDigitCount(long value) {
        int cnt = 0;

        while (value > 0) {
            value = value / 10;

            cnt++;
        }

        return cnt;
    }
}

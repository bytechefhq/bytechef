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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.TIME_ZONE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIX_TIMESTAMP;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.date.helper.util.DateHelperUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Monika Kušter
 */
public class DateHelperGetCurrentDateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getCurrentDate")
        .title("Get Current Date")
        .description("Get current date in the specified format.")
        .properties(
            string(TIME_ZONE)
                .label("Time Zone")
                .description("Time zone to use when formatting date.")
                .options((OptionsFunction<String>) DateHelperUtils::getZoneOptions)
                .required(true),
            DATE_FORMAT_PROPERTY)
        .output(outputSchema(string().description("Current date in the specified time zone and format.")))
        .perform(DateHelperGetCurrentDateAction::perform);

    private DateHelperGetCurrentDateAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String dateFormat = inputParameters.getRequiredString(DATE_FORMAT);
        String timeZone = inputParameters.getRequiredString(TIME_ZONE);

        if (dateFormat.equals(UNIX_TIMESTAMP)) {
            LocalDateTime localDateTime = LocalDateTime.now();
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZone));

            return zonedDateTime.toEpochSecond();
        } else {
            LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of(timeZone));
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat);

            return dateTimeFormatter.format(localDateTime);
        }
    }
}

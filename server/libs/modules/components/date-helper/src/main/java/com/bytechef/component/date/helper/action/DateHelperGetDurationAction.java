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

import static com.bytechef.component.date.helper.constants.DateHelperConstants.DAY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DURATION;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.HOUR;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MINUTE;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.MONTH;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.SECOND;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.UNIT_PROPERTY;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.YEAR;
import static com.bytechef.component.date.helper.util.DateHelperUtils.formatDuration;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class DateHelperGetDurationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDuration")
        .title("Get Duration")
        .description(
            "Given a total number of seconds, minutes, etc. – return human readable text containing how long the " +
                "duration was approximately, in the units of your choosing.")
        .properties(
            integer(DURATION)
                .label("Duration")
                .description("The duration value.")
                .required(true),
            UNIT_PROPERTY)
        .output(outputSchema(string().description("Human readable duration text.")))
        .help("", "https://docs.bytechef.io/reference/components/date-helper_v1#get-duration")
        .perform(DateHelperGetDurationAction::perform);

    private DateHelperGetDurationAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        int duration = inputParameters.getRequiredInteger(DURATION);
        String durationUnit = inputParameters.getRequiredString(UNIT);

        long totalSeconds = convertToSeconds(duration, durationUnit);

        return formatDuration(totalSeconds);
    }

    private static long convertToSeconds(int duration, String unit) {
        return switch (unit) {
            case SECOND -> duration;
            case MINUTE -> duration * 60L;
            case HOUR -> duration * 3600L;
            case DAY -> duration * 86400L;
            case MONTH -> duration * 2592000L;
            case YEAR -> duration * 31536000L;
            default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
        };
    }
}

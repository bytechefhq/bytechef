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
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT;
import static com.bytechef.component.date.helper.constants.DateHelperConstants.DATE_FORMAT_PROPERTY;
import static com.bytechef.component.date.helper.util.DateHelperUtils.getFormattedDate;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class DateHelperFormatDateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("formatDate")
        .title("Format Date")
        .description("Format date to a desired format.")
        .properties(
            dateTime(DATE)
                .label("Date")
                .description("Date which you want to format")
                .required(true),
            DATE_FORMAT_PROPERTY)
        .output(outputSchema(object().description("Date in desired format.")))
        .perform(DateHelperFormatDateAction::perform);

    private DateHelperFormatDateAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return getFormattedDate(
            inputParameters.getRequiredString(DATE_FORMAT),
            inputParameters.getRequiredLocalDateTime(DATE));
    }
}

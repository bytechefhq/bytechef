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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.FIRST_NAME;
import static com.bytechef.component.brevo.constant.BrevoConstants.LAST_NAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;

import com.bytechef.component.brevo.util.BrevoUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrevoUpdateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateContact")
        .title("Update contact")
        .description("Update contact.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("Email address of the user.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BrevoUtils::getContactsOptions)
                .required(true),
            string(FIRST_NAME)
                .label("First name")
                .description("First name of the user.")
                .required(true),
            string(LAST_NAME)
                .label("Last name")
                .description("Last name of the user.")
                .required(true))
        .perform(BrevoUpdateContactAction::perform);

    private BrevoUpdateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.put("/contacts/" + inputParameters.getRequiredString(EMAIL)))
            .body(Body.of(
                "attributes", Map.of(
                    FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                    LAST_NAME, inputParameters.getRequiredString(LAST_NAME))))
            .execute();
    }
}

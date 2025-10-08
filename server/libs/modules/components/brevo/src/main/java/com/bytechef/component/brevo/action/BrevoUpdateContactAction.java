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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.FIRST_NAME;
import static com.bytechef.component.brevo.constant.BrevoConstants.LAST_NAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;

import com.bytechef.component.brevo.util.BrevoUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class BrevoUpdateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateContact")
        .title("Update Contact")
        .description("Updates existing contact.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("Email address of the contact to update.")
                .options((OptionsFunction<String>) BrevoUtils::getContactsOptions)
                .required(true),
            string(FIRST_NAME)
                .label("First Name")
                .description("New first name of the contact.")
                .required(false),
            string(LAST_NAME)
                .label("Last Name")
                .description("New last name of the contact.")
                .required(false))
        .perform(BrevoUpdateContactAction::perform);

    private BrevoUpdateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context
            .http(http -> http.put("/contacts/" + inputParameters.getRequiredString(EMAIL)))
            .body(Body.of(
                "attributes",
                new Object[] {
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME)
                }))
            .execute();

        return null;
    }
}

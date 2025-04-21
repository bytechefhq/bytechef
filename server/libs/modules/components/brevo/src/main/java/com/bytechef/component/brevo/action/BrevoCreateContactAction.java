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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrevoCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates new contact.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("Email address of the contact.")
                .required(true),
            string(FIRST_NAME)
                .label("First Name")
                .description("First name of the contact.")
                .required(false),
            string(LAST_NAME)
                .label("Last Name")
                .description("Last name of the contact.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("id")
                            .description("ID of the created contact."))))
        .perform(BrevoCreateContactAction::perform);

    private BrevoCreateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, String> attributesMap = createAttributesMap(inputParameters);

        return context
            .http(http -> http.post("/contacts/"))
            .body(
                Body.of(
                    EMAIL, inputParameters.getRequiredString(EMAIL),
                    "attributes", attributesMap))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, String> createAttributesMap(Parameters inputParameters) {
        Map<String, String> attributesMap = new HashMap<>();

        String firstNAme = inputParameters.getString(FIRST_NAME);
        if (firstNAme != null) {
            attributesMap.put(FIRST_NAME, firstNAme);
        }

        String lastName = inputParameters.getString(LAST_NAME);
        if (lastName != null) {
            attributesMap.put(LAST_NAME, lastName);
        }

        return attributesMap;
    }
}

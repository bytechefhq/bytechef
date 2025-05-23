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

package com.bytechef.component.insightly.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class InsightlyCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates new contact.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/Contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("FIRST_NAME").maxLength(255)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("First Name")
            .description("The first name of the contact.")
            .required(true),
            string("LAST_NAME").maxLength(255)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Last Name")
                .description("The last name of the contact.")
                .required(false),
            string("EMAIL_ADDRESS").maxLength(255)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Email Address")
                .description("Email address of the contact.")
                .required(false),
            string("PHONE").maxLength(255)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Phone")
                .description("Phone number of the contact.")
                .required(false),
            string("TITLE").maxLength(255)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Title")
                .description("The contact's title in company.")
                .required(false))
        .output(outputSchema(object().properties(integer("CONTACT_ID").description("ID of the contact.")
            .required(false),
            string("FIRST_NAME").description("First name of the contact.")
                .required(false),
            string("LAST_NAME").description("Last name of the contact.")
                .required(false),
            string("EMAIL_ADDRESS").description("Email address of the contact.")
                .required(false),
            string("PHONE").description("Phone number of the contact.")
                .required(false),
            string("TITLE").description("The contact's title in company.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private InsightlyCreateContactAction() {
    }
}

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

package com.bytechef.component.insightly.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class InsightlyCreateContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create contact")
        .description("Creates new Contact")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/Contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("FIRST_NAME").maxLength(255)
            .label("First   Name")
            .description("The first name of the contact")
            .required(true),
            string("LAST_NAME").maxLength(255)
                .label("Last   Name")
                .description("The last name of the contact")
                .required(false),
            string("EMAIL_ADDRESS").maxLength(255)
                .label("Email   Address")
                .description("Email address of the contact")
                .required(false),
            string("PHONE").maxLength(255)
                .label("Phone")
                .description("Phone number of the contact")
                .required(false),
            string("TITLE").maxLength(255)
                .label("Title")
                .description("The contact's title in company.")
                .required(false))
            .label("Contact")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(integer("CONTACT_ID").required(false), string("FIRST_NAME").required(false),
                string("LAST_NAME").required(false), string("EMAIL_ADDRESS").required(false),
                string("PHONE").required(false), string("TITLE").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private InsightlyCreateContactAction() {
    }
}

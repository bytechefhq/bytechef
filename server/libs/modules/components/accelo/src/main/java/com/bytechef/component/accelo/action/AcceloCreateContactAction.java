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

package com.bytechef.component.accelo.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.accelo.util.AcceloUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AcceloCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("firstname").label("First Name")
            .description("The first name of the contact.")
            .required(false),
            string("surname").label("Last Name")
                .description("The last name of the contact.")
                .required(false),
            string("company_id").label("Company ID")
                .description("ID of the company  to which the newly affiliated contact will be linked.")
                .required(true)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AcceloUtils::getCompanyIdOptions),
            string("phone").label("Phone")
                .description("The contact's phone number in their role in the associated company.")
                .required(false),
            string("email").label("Email")
                .description("The contact's position in the associated company.")
                .required(false))
            .label("Contact")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(
                    object("response")
                        .properties(string("id").required(false), string("firstname").required(false),
                            string("lastname").required(false), string("email").required(false))
                        .required(false),
                    object("meta")
                        .properties(string("more_info").required(false), string("status").required(false),
                            string("message").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AcceloCreateContactAction() {
    }
}

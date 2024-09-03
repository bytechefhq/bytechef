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

package com.bytechef.component.hubspot.action;

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
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
public class HubspotCreateContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Create a contact with the given properties.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/crm/v3/objects/contacts", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(object("__item")
            .properties(object("properties").properties(string("firstname").label("First   Name")
                .required(false),
                string("lastname").label("Last   Name")
                    .required(false),
                string("email").label("Email   Address")
                    .required(false),
                string("phone").label("Phone   Number")
                    .required(false),
                string("company").label("Company")
                    .description("Company contact belongs to.")
                    .required(false),
                string("website").label("Website")
                    .description("Website of the contact.")
                    .required(false))
                .label("Properties")
                .required(false))
            .label("Contact")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false),
                    object("properties")
                        .properties(string("firstname").required(false), string("lastname").required(false),
                            string("email").required(false), string("phone").required(false),
                            string("company").required(false), string("website").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private HubspotCreateContactAction() {
    }
}

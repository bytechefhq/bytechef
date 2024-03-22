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

package com.bytechef.component.pipeliner.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
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
public class PipelinerCreateContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create contact")
        .description("Creates new Contact")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/entities/Contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("owner_id").label("Owner Id")
            .description("User in Pipeliner Application that will become the owner of the newly created Contact.")
            .required(true),
            string("first_name").label("First Name")
                .description("The firstname of the contact.")
                .required(false),
            string("last_name").label("Last Name")
                .description("The lastname of the contact.")
                .required(true))
            .label("Contact")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(bool("success").description("True when response succeeded, false on error.")
            .required(false),
            object("data")
                .properties(string("id").required(false), string("owner_id").required(false),
                    string("first_name").required(false), string("last_name").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}

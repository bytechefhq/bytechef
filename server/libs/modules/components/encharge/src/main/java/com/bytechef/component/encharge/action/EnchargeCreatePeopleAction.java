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

package com.bytechef.component.encharge.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
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
public class EnchargeCreatePeopleAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createPeople")
        .title("Create people")
        .description("Creates new People")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/people", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(array("__items").items(object().properties(string("email").label("Email")
            .description("The person's email address.")
            .required(true),
            string("firstName").label("First Name")
                .description("The first name of the person.")
                .required(false),
            string("lastName").label("Last Name")
                .description("The last name of the person.")
                .required(false),
            string("website").label("Website")
                .description("The person's website.")
                .required(false),
            string("title").label("Title")
                .description("Title of the person")
                .required(false),
            string("phone").label("Phone")
                .description("The person's phone number")
                .required(false)))
            .placeholder("Add to Items")
            .label("People")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(array("users")
                    .items(object().properties(string("email").required(false), string("firstName").required(false),
                        string("lastName").required(false), string("website").required(false),
                        string("title").required(false), string("id").required(false), string("phone").required(false)))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}

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

package com.bytechef.component.petstore.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.petstore.property.PetstoreUserProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PetstoreUpdateUserAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateUser")
        .title("Update user")
        .description("This can only be done by the logged in user.")
        .metadata(
            Map.of(
                "method", "PUT",
                "path", "/user/{username}", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("username").label("Username")
            .description("name that need to be deleted")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            integer("id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Id")
                .required(false)
                .exampleValue(10),
            string("username").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Username")
                .required(false)
                .exampleValue("theUser"),
            string("firstName").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("First Name")
                .required(false)
                .exampleValue("John"),
            string("lastName").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Last Name")
                .required(false)
                .exampleValue("James"),
            string("email").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Email")
                .required(false)
                .exampleValue("john@email.com"),
            string("password").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Password")
                .required(false)
                .exampleValue("12345"),
            string("phone").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Phone")
                .required(false)
                .exampleValue("12345"),
            integer("userStatus").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("User Status")
                .description("User Status")
                .required(false)
                .exampleValue(1))
        .output(outputSchema(object().properties(PetstoreUserProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PetstoreUpdateUserAction() {
    }
}

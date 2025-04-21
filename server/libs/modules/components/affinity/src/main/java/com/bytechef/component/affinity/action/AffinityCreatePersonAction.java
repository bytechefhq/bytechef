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

package com.bytechef.component.affinity.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
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
public class AffinityCreatePersonAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createPerson")
        .title("Create Person")
        .description("Creates a new person.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/persons", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("first_name").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("First Name")
            .description("The first name of the person.")
            .required(true),
            string("last_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Last Name")
                .description("The last name of the person.")
                .required(true),
            array("emails").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("The email addresses of the person."))
                .placeholder("Add to Emails")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Emails")
                .description("The email addresses of the person.")
                .required(false))
        .output(outputSchema(object().properties(string("id").description("The ID of the person.")
            .required(false),
            string("first_name").description("The first name of the person.")
                .required(false),
            string("last_name").description("The last name of the person.")
                .required(false),
            array("emails").items(string().description("The email addresses of the person."))
                .description("The email addresses of the person.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AffinityCreatePersonAction() {
    }
}

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

package com.bytechef.component.pipeliner.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.pipeliner.util.PipelinerUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipelinerCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates new contact.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/entities/Contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("owner_id").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Owner ID")
            .description(
                "ID of the user in Pipeliner Application that will become the owner of the newly created Contact.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) PipelinerUtils::getOwnerIdOptions),
            string("first_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("First Name")
                .description("The firstname of the contact.")
                .required(false),
            string("last_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Last Name")
                .description("The lastname of the contact.")
                .required(true))
        .output(outputSchema(object()
            .properties(bool("success").description("True when response succeeded, false on error.")
                .required(false),
                object("data").properties(string("id").description("ID of the contact.")
                    .required(false),
                    string("owner_id")
                        .description("ID of the user in Pipeliner Application that is the owner of the contact.")
                        .required(false),
                    string("first_name").description("First name of the contact.")
                        .required(false),
                    string("last_name").description("Last name of the contact.")
                        .required(false))
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipelinerCreateContactAction() {
    }
}

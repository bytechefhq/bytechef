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

package com.bytechef.component.teamwork.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
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
public class TeamworkCreateCompanyAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createCompany")
        .title("Create Company")
        .description("Creates a new company.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/companies.json", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("company").properties(string("name").label("Name")
            .description("The name of the company.")
            .required(false),
            string("emailOne").label("Email")
                .required(false),
            string("phone").label("Phone")
                .description("Phone number for the company.")
                .required(false),
            string("website").label("Website")
                .description("The company's website.")
                .required(false))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Company")
            .required(false))
        .output(outputSchema(object()
            .properties(string("name").required(false), string("emailOne").required(false),
                string("phone").required(false), string("website").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private TeamworkCreateCompanyAction() {
    }
}

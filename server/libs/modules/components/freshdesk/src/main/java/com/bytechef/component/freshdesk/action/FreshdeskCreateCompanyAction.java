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

package com.bytechef.component.freshdesk.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
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
public class FreshdeskCreateCompanyAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createCompany")
        .title("Create Company")
        .description("Creates a new compan.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/companies", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("name").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Name")
            .description("Name of the company.")
            .required(true),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("Description of the company.")
                .required(false),
            string("note").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Note")
                .description("Any specific note about the company.")
                .required(false))
        .output(outputSchema(object().properties(integer("id").description("ID of the company.")
            .required(false),
            string("name").description("Name of the company.")
                .required(false),
            string("description").description("Description of the company.")
                .required(false),
            array("domains").items(string().description("List of domains associated with the company."))
                .description("List of domains associated with the company.")
                .required(false),
            string("note").description("Note about the company.")
                .required(false),
            string("created_at").description("Timestamp when the company was created.")
                .required(false),
            string("updated_at").description("Timestamp when the company was last updated.")
                .required(false),
            string("health_score").description("Health score of the company.")
                .required(false),
            string("account_tier").description("Account tier of the company.")
                .required(false),
            string("renewal_date").description("Renewal date of the company subscription.")
                .required(false),
            string("industry").description("Industry of the company.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/freshdesk_v1#create-company");

    private FreshdeskCreateCompanyAction() {
    }
}

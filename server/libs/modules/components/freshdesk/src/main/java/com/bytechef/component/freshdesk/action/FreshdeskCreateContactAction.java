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
import static com.bytechef.component.definition.ComponentDsl.number;
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
public class FreshdeskCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("name").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Name")
            .description("Full name of the contact")
            .required(true),
            string("email").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Email")
                .description("Primary email address of the contact.")
                .required(true),
            string("phone").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Work Phone")
                .description("Telephone number of the contact.")
                .required(false),
            string("mobile").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Mobile")
                .description("Mobile number of the contact.")
                .required(false),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("A small description of the contact.")
                .required(false),
            string("job_title").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Job Title")
                .description("Job title of the contact.")
                .required(false))
        .output(outputSchema(object()
            .properties(string("description").description("A small description of the contact.")
                .required(false),
                string("email").description("Primary email address of the contact.")
                    .required(false),
                number("id").description("ID of the contact.")
                    .required(false),
                string("job_title").description("Job title of the contact.")
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private FreshdeskCreateContactAction() {
    }
}

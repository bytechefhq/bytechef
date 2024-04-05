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

package com.bytechef.component.freshdesk.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.number;
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
public class FreshdeskCreateContactAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create contact")
        .description("Creates a new contact")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("name").label("Name")
            .description("Full name of the contact")
            .required(true),
            string("email").label("Email")
                .description("Primary email address of the contact.")
                .required(true),
            string("phone").label("Work   Phone")
                .description("Telephone number of the contact")
                .required(false),
            string("mobile").label("Mobile")
                .description("Mobile number of the contact")
                .required(false),
            string("description").label("Description")
                .description("A small description of the contact")
                .required(false),
            string("job_title").label("Job Title")
                .description("Job title of the contact")
                .required(false))
            .label("Contact")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("description").required(false), string("email").required(false),
                    number("id").required(false), string("job_title").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private FreshdeskCreateContactAction() {
    }
}

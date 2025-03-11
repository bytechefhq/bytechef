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

package com.bytechef.component.active.campaign.action;

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
public class ActiveCampaignCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("contact").properties(string("email").label("Email")
            .description("Email address of the new contact.")
            .required(true)
            .exampleValue("test@example.com"),
            string("firstName").label("First Name")
                .description("First name of the new contact.")
                .required(false),
            string("lastName").label("Last Name")
                .description("Last name of the new contact.")
                .required(false),
            string("phone").label("Phone")
                .description("Phone number of the contact.")
                .required(false))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Contact")
            .required(false))
        .output(
            outputSchema(
                object()
                    .properties(object("contact")
                        .properties(string("email").description("Email address of the contact.")
                            .required(false),
                            string("firstName").description("First name of the contact.")
                                .required(false),
                            string("lastName").description("Last name of the contact.")
                                .required(false),
                            string("phone").description("Phone number of the contact.")
                                .required(false),
                            string("id").description("ID of the contact.")
                                .required(false))
                        .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))));

    private ActiveCampaignCreateContactAction() {
    }
}

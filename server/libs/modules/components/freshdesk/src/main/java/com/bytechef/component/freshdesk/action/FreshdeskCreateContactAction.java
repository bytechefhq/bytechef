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
import static com.bytechef.component.definition.ComponentDsl.bool;
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
public class FreshdeskCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
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
        .output(outputSchema(object().properties(bool("active").description("Whether the contact is active.")
            .required(false),
            string("address").description("Address of the contact.")
                .required(false),
            integer("company_id").description("ID of the primary company of the contact.")
                .required(false),
            bool("view_all_tickets").description("Whether the contact can view all tickets.")
                .required(false),
            bool("deleted").description("Whether the contact is deleted.")
                .required(false),
            string("description").description("Description of the contact.")
                .required(false),
            string("email").description("Email address of the contact.")
                .required(false),
            integer("id").description("ID of the contact.")
                .required(false),
            string("contact_type").description("Type of the contact.")
                .required(false),
            string("job_title").description("Job title of the contact.")
                .required(false),
            string("language").description("Language of the contact.")
                .required(false),
            string("mobile").description("Mobile number of the contact.")
                .required(false),
            string("name").description("Name of the contact.")
                .required(false),
            string("phone").description("Phone number of the contact.")
                .required(false),
            string("time_zone").description("Time zone of the contact.")
                .required(false),
            string("twitter_id").description("Twitter ID of the contact.")
                .required(false),
            array("social_handler").items(string().description("List of social handlers of the contact."))
                .description("List of social handlers of the contact.")
                .required(false),
            array("other_emails").items(string().description("List of additional email addresses of the contact."))
                .description("List of additional email addresses of the contact.")
                .required(false),
            array("other_companies").items(object().properties(integer("company_id").description("ID of the company.")
                .required(false),
                bool("view_all_tickets").description("Whether the contact can view all tickets of the company.")
                    .required(false))
                .description("List of other companies associated with the contact."))
                .description("List of other companies associated with the contact.")
                .required(false),
            string("created_at").description("Timestamp when the contact was created.")
                .required(false),
            string("updated_at").description("Timestamp when the contact was last updated.")
                .required(false),
            array("tags").items(string().description("List of tags associated with the contact."))
                .description("List of tags associated with the contact.")
                .required(false),
            string("avatar").description("Avatar of the contact.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/freshdesk_v1#create-contact");

    private FreshdeskCreateContactAction() {
    }
}

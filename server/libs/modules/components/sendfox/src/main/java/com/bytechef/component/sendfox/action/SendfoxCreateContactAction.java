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

package com.bytechef.component.sendfox.action;

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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.sendfox.util.SendfoxUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SendfoxCreateContactAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/contacts", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("email").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Email Address")
            .description("Email address of the contact.")
            .required(true),
            string("first_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("First Name")
                .description("First name of the contact.")
                .required(false),
            string("last_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Last Name")
                .description("Last name of the contact.")
                .required(false),
            array("lists").items(integer(null).metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("Lists to which the new contact will be added."))
                .placeholder("Add to Lists")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Lists")
                .description("Lists to which the new contact will be added.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<Long>) SendfoxUtils::getListsOptions))
        .output(outputSchema(object().properties(integer("id").description("ID of the contact.")
            .required(false),
            string("email").description("Email of the contact.")
                .required(false),
            string("first_name").description("First name of the contact.")
                .required(false),
            string("last_name").description("Last name of the contact.")
                .required(false),
            string("ip_address").description("IP address of the contact.")
                .required(false),
            string("unsubscribed_at").description("Date when the contact unsubscribed.")
                .required(false),
            string("bounced_at").description("Date when the contact was bounced.")
                .required(false),
            string("created_at").description("Date when the contact was created.")
                .required(false),
            string("updated_at").description("Date when the contact was updated.")
                .required(false),
            integer("form_id").description("Form ID of the contact.")
                .required(false),
            integer("contact_import_id").description("Contact import ID of the contact.")
                .required(false),
            bool("via_api").description("Whether the contact was created via API.")
                .required(false),
            string("last_opened_at").description("Date when the contact last opened an email.")
                .required(false),
            string("last_clicked_at").description("Date when the contact last clicked an email.")
                .required(false),
            string("first_sent_at").description("Date when the first email was sent to the contact.")
                .required(false),
            string("last_sent_at").description("Date when the last email was sent to the contact.")
                .required(false),
            string("invalid_at").description("Date when the contact became invalid.")
                .required(false),
            string("inactive_at").description("Date when the contact became invalid.")
                .required(false),
            string("confirmed_at").description("Date when the contact confirmed the subscription.")
                .required(false),
            integer("social_platform_id").description("Social platform ID of the contact.")
                .required(false),
            string("confirmation_sent_at")
                .description("Date when the confirmation for the subscription was sent to the contact.")
                .required(false),
            integer("confirmation_sent_count").description("How many confirmation emails were sent.")
                .required(false),
            string("created_ago").description("How many seconds ago was the contact created.")
                .required(false),
            array("contact_fields")
                .items(object().properties(string("name").description("Name of the additional contact field.")
                    .required(false),
                    string("value").description("Value of the additional contact field.")
                        .required(false))
                    .description("Additional contact information."))
                .description("Additional contact information.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private SendfoxCreateContactAction() {
    }
}

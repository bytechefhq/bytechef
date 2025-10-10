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

package com.bytechef.component.hunter.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.hunter.util.HunterUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class HunterCreateLeadAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createLead")
        .title("Create Lead")
        .description("Creates a new lead.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/leads", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("email").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Email Address")
            .description("The email address of the lead.")
            .required(false),
            string("first_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("First Name")
                .description("The first name of the lead.")
                .required(false),
            string("last_name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Last Name")
                .description("The last name of the lead.")
                .required(false),
            string("position").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Position")
                .description("The job title of the lead.")
                .required(false),
            string("company").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Company")
                .description("The name of the company the lead is working in.")
                .required(false),
            string("phone_number").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Phone Number")
                .description("The phone number of the lead.")
                .required(false),
            integer("lead_list_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Lead List ID")
                .description(
                    "The identifier of the list the lead belongs to. If it's not specified, the lead is saved in the last list created.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<Long>) HunterUtils::getLeadListIdOptions))
        .output(outputSchema(object().properties(object("data").properties(string("id").description("ID of the lead.")
            .required(false),
            string("email").description("Email address of the lead.")
                .required(false),
            string("first_name").description("First name of the lead.")
                .required(false),
            string("last_name").description("Last name of the lead.")
                .required(false),
            string("position").description("Job title of the lead.")
                .required(false),
            string("company").description("Name of the company the lead is working in.")
                .required(false),
            object("leads_list").properties(integer("id").description("ID of the list the lead belongs to.")
                .required(false),
                string("name").description("Name of the list the lead belongs to.")
                    .required(false))
                .required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HunterCreateLeadAction() {
    }
}

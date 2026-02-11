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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.pipedrive.util.PipedriveUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveAddPersonAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addPerson")
        .title("Add Person")
        .description("Adds a new person.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/persons", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("name").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Name")
            .description("Person full name")
            .required(true),
            integer("owner_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Owner ID")
                .description("ID of the user who will be marked as the owner of this person.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<Long>) PipedriveUtils::getOwnerIdOptions),
            integer("org_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Organization ID")
                .description("ID of the organization this person will belong to.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<Long>) PipedriveUtils::getOrgIdOptions),
            array("email").items(object().properties(string("value").label("Email Address")
                .required(true),
                bool("primary").label("Primary")
                    .description("If email is primary for the person or not.")
                    .required(false),
                string("label").label("Type")
                    .description("Type of the email")
                    .options(option("Work", "work"), option("Home", "home"), option("Other", "other"))
                    .required(false))
                .description("An email addresses related to the person."))
                .placeholder("Add to Email")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Email")
                .description("An email addresses related to the person.")
                .required(false),
            array("phone").items(object().properties(string("value").label("Phone Number")
                .description("The phone number")
                .required(true),
                bool("primary").label("Primary")
                    .description("If phone number is primary for the person or not.")
                    .required(false),
                string("label").label("Type")
                    .description("Type of the phone number.")
                    .options(option("Work", "work"), option("Home", "home"), option("Mobile", "mobile"),
                        option("Other", "other"))
                    .required(false))
                .description("A phone numbers related to the person."))
                .placeholder("Add to Phone")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Phone")
                .description("A phone numbers related to the person.")
                .required(false))
        .output(outputSchema(object()
            .properties(object("data")
                .properties(integer("id").required(false), integer("company_id").required(false),
                    object("owner_id")
                        .properties(integer("id").required(false), string("name").required(false),
                            string("email").required(false))
                        .required(false),
                    object("org_id")
                        .properties(string("name").required(false), integer("owner_id").required(false),
                            string("cc_email").required(false))
                        .required(false),
                    string("name").required(false),
                    array("phone")
                        .items(object().properties(string("value").required(false), bool("primary").required(false),
                            string("label").required(false)))
                        .required(false),
                    array("email")
                        .items(object().properties(string("value").required(false), bool("primary").required(false),
                            string("label").required(false)))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipedriveAddPersonAction() {
    }
}

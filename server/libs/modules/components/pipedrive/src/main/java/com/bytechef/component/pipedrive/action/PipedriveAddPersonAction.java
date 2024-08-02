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

package com.bytechef.component.pipedrive.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
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
public class PipedriveAddPersonAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addPerson")
        .title("Add person")
        .description("Adds a new person.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/persons", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("name").label("Name")
            .description("Person full name")
            .required(true),
            integer("owner_id").label("Owner")
                .description("User who will be marked as the owner of this person.")
                .required(false),
            integer("org_id").label("Organization")
                .description("Organization this person will belong to.")
                .required(false),
            array("email").items(object().properties(string("value").label("Email   Address")
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
                .label("Email")
                .description("An email addresses related to the person.")
                .required(false),
            array("phone").items(object().properties(string("value").label("Phone   Number")
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
                .label("Phone")
                .description("A phone numbers related to the person.")
                .required(false))
            .label("Person")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
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
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private PipedriveAddPersonAction() {
    }
}

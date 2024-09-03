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

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveSearchPersonsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchPersons")
        .title("Search persons")
        .description("Searches all persons by name, email, phone, notes and/or custom fields.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/persons/search"

            ))
        .properties(string("term").label("Term")
            .description(
                "The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("fields").label("Fields")
                .description(
                    "A comma-separated string array. The fields to perform the search from. Defaults to all of them.")
                .options(option("Custom_fields", "custom_fields"), option("Email", "email"), option("Notes", "notes"),
                    option("Phone", "phone"), option("Name", "name"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("exact_match").label("Exact Match")
                .description(
                    "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization")
                .description("Will filter persons by the provided organization.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(object("data")
                    .properties(array("items")
                        .items(object().properties(integer("id").required(false), integer("company_id").required(false),
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
                                .items(object().properties(string("value").required(false),
                                    bool("primary").required(false), string("label").required(false)))
                                .required(false),
                            array("email")
                                .items(object().properties(string("value").required(false),
                                    bool("primary").required(false), string("label").required(false)))
                                .required(false)))
                        .required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private PipedriveSearchPersonsAction() {
    }
}

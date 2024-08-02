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
import static com.bytechef.component.definition.ComponentDSL.date;
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
public class PipedriveSearchLeadsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchLeads")
        .title("Search leads")
        .description("Searches all leads by title, notes and/or custom fields.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/leads/search"

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
                .options(option("Custom_fields", "custom_fields"), option("Notes", "notes"), option("Title", "title"))
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
            integer("person_id").label("Person")
                .description("Will filter leads by the provided person ID.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization")
                .description("Will filter leads by the provided organization ID.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("include_fields").label("Include Fields")
                .description("Supports including optional fields in the results which are not provided by default.")
                .options(option("Lead.was_seen", "lead.was_seen"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(
            object()
                .properties(
                    object("body")
                        .properties(object("data")
                            .properties(array("items")
                                .items(object().properties(string("id").required(false),
                                    string("title").required(false), integer("owner_id").required(false),
                                    object("value")
                                        .properties(integer("amount").required(false),
                                            string("currency").required(false))
                                        .required(false),
                                    date("expected_close_date").required(false), integer("person_id").required(false)))
                                .required(false))
                            .required(false))
                        .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON)));

    private PipedriveSearchLeadsAction() {
    }
}

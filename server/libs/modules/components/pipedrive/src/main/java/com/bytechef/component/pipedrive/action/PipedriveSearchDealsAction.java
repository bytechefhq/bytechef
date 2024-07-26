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
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveSearchDealsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchDeals")
        .title("Search deals")
        .description("Searches all deals by title, notes and/or custom fields.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/deals/search"

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
                .description("Will filter deals by the provided person.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization")
                .description("Will filter deals by the provided organization.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("status").label("Status")
                .description("Will filter deals by the provided specific status.")
                .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("include_fields").label("Include Fields")
                .description("Supports including optional fields in the results which are not provided by default.")
                .options(option("Deal.cc_email", "deal.cc_email"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(object("data")
                    .properties(array("items")
                        .items(object().properties(string("id").required(false), string("type").required(false),
                            object("user_id")
                                .properties(integer("id").required(false), string("name").required(false),
                                    string("email").required(false))
                                .required(false),
                            object("person_id").properties(string("name").required(false))
                                .required(false),
                            object("org_id")
                                .properties(string("name").required(false), string("owner_id").required(false))
                                .required(false),
                            integer("stage_id").required(false), string("title").required(false),
                            integer("value").required(false), string("currency").required(false),
                            string("status").required(false)))
                        .required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private PipedriveSearchDealsAction() {
    }
}

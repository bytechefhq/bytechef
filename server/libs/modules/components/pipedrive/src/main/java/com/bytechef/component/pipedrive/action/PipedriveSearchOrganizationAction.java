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
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveSearchOrganizationAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchOrganization")
        .title("Search organizations")
        .description(
            "Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/organizations/search"

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
                .options(option("Address", "address"), option("Custom_fields", "custom_fields"),
                    option("Notes", "notes"), option("Name", "name"))
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
            integer("start").label("Start")
                .description(
                    "Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("limit").label("Limit")
                .description("Items shown per page")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(
            object("data")
                .properties(array("items")
                    .items(number("result_score").required(false),
                        object("item")
                            .properties(integer("id").required(false), string("type").required(false),
                                string("name").required(false), string("address").required(false),
                                integer("visible_to").required(false),
                                object("owner").properties(integer("id").required(false))
                                    .required(false),
                                array("custom_fields").items(string())
                                    .required(false),
                                array("notes").items(string())
                                    .required(false))
                            .required(false))
                    .required(false))
                .required(false),
            bool("success").required(false),
            object("additional_data")
                .properties(object("pagination")
                    .properties(integer("start").required(false), integer("limit").required(false),
                        bool("more_items_in_collection").required(false), integer("next_start").required(false))
                    .required(false))
                .required(false))
        .outputSchemaMetadata(Map.of(
            "responseType", ResponseType.JSON))
        .sampleOutput(
            Map.<String, Object>ofEntries(
                Map.entry("success", true), Map
                    .entry("data",
                        Map.<String, Object>ofEntries(Map.entry("items",
                            List.of(Map.<String, Object>ofEntries(Map.entry("result_score", 0.316),
                                Map.entry("item", Map.<String, Object>ofEntries(Map.entry("id", 1),
                                    Map.entry("type", "organization"), Map.entry("name", "Organization name"),
                                    Map.entry("address", "Mustamäe tee 3a, 10615 Tallinn"), Map.entry("visible_to", 3),
                                    Map.entry("owner", Map.<String, Object>ofEntries(Map.entry("id", 1))),
                                    Map.entry("custom_fields", List.of()), Map.entry("notes", List.of())))))))),
                Map.entry("additional_data",
                    Map.<String, Object>ofEntries(
                        Map.entry("pagination", Map.<String, Object>ofEntries(Map.entry("start", 0),
                            Map.entry("limit", 100), Map.entry("more_items_in_collection", false)))))));
}


/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SearchOrganizationAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchOrganization")
        .display(
            display("Search organizations")
                .description(
                    "Searches all organizations by name, address, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
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
            bool("exact_match").label("Exact_match")
                .description(
                    "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                .options(option("True", true), option("False", false))
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
        .output(object(null)
            .properties(
                object("data")
                    .properties(array("items")
                        .items(object(null).properties(number("result_score").label("Result_score")
                            .description("Search result relevancy")
                            .required(false),
                            object("item").properties(integer("id").label("Id")
                                .description("The ID of the organization")
                                .required(false),
                                string("type").label("Type")
                                    .description("The type of the item")
                                    .required(false),
                                string("name").label("Name")
                                    .description("The name of the organization")
                                    .required(false),
                                string("address").label("Address")
                                    .description("The address of the organization")
                                    .required(false),
                                integer("visible_to").label("Visible_to")
                                    .description("The visibility of the organization")
                                    .required(false),
                                object("owner").properties(integer("id").label("Id")
                                    .description("The ID of the owner of the deal")
                                    .required(false))
                                    .label("Owner")
                                    .required(false),
                                array("custom_fields").items(string(null).description("Custom fields"))
                                    .placeholder("Add")
                                    .label("Custom_fields")
                                    .description("Custom fields")
                                    .required(false),
                                array("notes").items(string(null).description("An array of notes"))
                                    .placeholder("Add")
                                    .label("Notes")
                                    .description("An array of notes")
                                    .required(false))
                                .label("Item")
                                .required(false))
                            .description("The array of found items"))
                        .placeholder("Add")
                        .label("Items")
                        .description("The array of found items")
                        .required(false))
                    .label("Data")
                    .required(false),
                bool("success").label("Success")
                    .description("If the response is successful or not")
                    .required(false),
                object("additional_data").properties(object("pagination").properties(integer("start").label("Start")
                    .description("Pagination start")
                    .required(false),
                    integer("limit").label("Limit")
                        .description("Items shown per page")
                        .required(false),
                    bool("more_items_in_collection").label("More_items_in_collection")
                        .description("Whether there are more list items in the collection than displayed")
                        .required(false),
                    integer("next_start").label("Next_start")
                        .description("Next pagination start")
                        .required(false))
                    .label("Pagination")
                    .description("Pagination details of the list")
                    .required(false))
                    .label("Additional_data")
                    .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"success\":true,\"data\":{\"items\":[{\"result_score\":0.316,\"item\":{\"id\":1,\"type\":\"organization\",\"name\":\"Organization name\",\"address\":\"Mustamäe tee 3a, 10615 Tallinn\",\"visible_to\":3,\"owner\":{\"id\":1},\"custom_fields\":[],\"notes\":[]}}]},\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}");
}


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
public class SearchLeadsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchLeads")
        .display(
            display("Search leads")
                .description(
                    "Searches all leads by title, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope. Found leads can be filtered by the person ID and the organization ID."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
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
            bool("exact_match").label("Exact_match")
                .description(
                    "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                .options(option("True", true), option("False", false))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("person_id").label("Person_id")
                .description(
                    "Will filter leads by the provided person ID. The upper limit of found leads associated with the person is 2000.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization_id")
                .description(
                    "Will filter leads by the provided organization ID. The upper limit of found leads associated with the organization is 2000.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("include_fields").label("Include_fields")
                .description("Supports including optional fields in the results which are not provided by default")
                .options(option("Lead.was_seen", "lead.was_seen"))
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
                            object("item").properties(string("id").label("Id")
                                .description("The ID of the lead")
                                .required(false),
                                string("type").label("Type")
                                    .description("The type of the item")
                                    .required(false),
                                string("title").label("Title")
                                    .description("The title of the lead")
                                    .required(false),
                                object("owner").properties(integer("id").label("Id")
                                    .description("The ID of the owner of the lead")
                                    .required(false))
                                    .label("Owner")
                                    .required(false),
                                object("person").properties(integer("id").label("Id")
                                    .description("The ID of the person the lead is associated with")
                                    .required(false),
                                    string("name").label("Name")
                                        .description("The name of the person the lead is associated with")
                                        .required(false))
                                    .label("Person")
                                    .required(false),
                                object("organization").properties(integer("id").label("Id")
                                    .description("The ID of the organization the lead is associated with")
                                    .required(false),
                                    string("name").label("Name")
                                        .description("The name of the organization the lead is associated with")
                                        .required(false))
                                    .label("Organization")
                                    .required(false),
                                array("phones").items(string(null))
                                    .placeholder("Add")
                                    .label("Phones")
                                    .required(false),
                                array("emails").items(string(null))
                                    .placeholder("Add")
                                    .label("Emails")
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
                                    .required(false),
                                integer("value").label("Value")
                                    .description("The value of the lead")
                                    .required(false),
                                string("currency").label("Currency")
                                    .description("The currency of the lead")
                                    .required(false),
                                integer("visible_to").label("Visible_to")
                                    .description("The visibility of the lead")
                                    .required(false),
                                bool("is_archived").label("Is_archived")
                                    .description("A flag indicating whether the lead is archived or not")
                                    .required(false))
                                .label("Item")
                                .required(false))
                            .description("The array of leads"))
                        .placeholder("Add")
                        .label("Items")
                        .description("The array of leads")
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
            "{\"success\":true,\"data\":{\"items\":[{\"result_score\":0.29,\"item\":{\"id\":\"39c433f0-8a4c-11ec-8728-09968f0a1ca0\",\"type\":\"lead\",\"title\":\"John Doe lead\",\"owner\":{\"id\":1},\"person\":{\"id\":1,\"name\":\"John Doe\"},\"organization\":{\"id\":1,\"name\":\"John company\"},\"phones\":[],\"emails\":[\"john@doe.com\"],\"custom_fields\":[],\"notes\":[],\"value\":100,\"currency\":\"USD\",\"visible_to\":3,\"is_archived\":false}}]},\"additional_data\":{\"description\":\"The additional data of the list\",\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"integer\",\"description\":\"Pagination start\"},\"limit\":{\"type\":\"integer\",\"description\":\"Items shown per page\"},\"more_items_in_collection\":{\"type\":\"boolean\",\"description\":\"If there are more list items in the collection than displayed or not\"}}}}");
}

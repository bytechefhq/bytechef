
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
public class PipedriveSearchDealsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchDeals")
        .title("Search deals")
        .description(
            "Searches all deals by title, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope. Found deals can be filtered by the person ID and the organization ID.")
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
            integer("person_id").label("Person Id")
                .description(
                    "Will filter deals by the provided person ID. The upper limit of found deals associated with the person is 2000.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization Id")
                .description(
                    "Will filter deals by the provided organization ID. The upper limit of found deals associated with the organization is 2000.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("status").label("Status")
                .description(
                    "Will filter deals by the provided specific status. open = Open, won = Won, lost = Lost. The upper limit of found deals associated with the status is 2000.")
                .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("include_fields").label("Include Fields")
                .description("Supports including optional fields in the results which are not provided by default")
                .options(option("Deal.cc_email", "deal.cc_email"))
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
            object()
                .properties(
                    object(
                        "data")
                            .properties(
                                array(
                                    "items")
                                        .items(
                                            object()
                                                .properties(
                                                    number("result_score").description("Search result relevancy")
                                                        .required(false),
                                                    object("item")
                                                        .properties(integer("id").description("The ID of the deal")
                                                            .required(false),
                                                            string("type").description("The type of the item")
                                                                .required(false),
                                                            string("title").description("The title of the deal")
                                                                .required(false),
                                                            integer("value").description("The value of the deal")
                                                                .required(false),
                                                            string("currency").description("The currency of the deal")
                                                                .required(false),
                                                            string("status").description("The status of the deal")
                                                                .required(false),
                                                            integer("visible_to")
                                                                .description("The visibility of the deal")
                                                                .required(false),
                                                            object("owner").properties(integer("id")
                                                                .description("The ID of the owner of the deal")
                                                                .required(false))
                                                                .required(false),
                                                            object("stage")
                                                                .properties(
                                                                    integer("id")
                                                                        .description("The ID of the stage of the deal")
                                                                        .required(false),
                                                                    string("name")
                                                                        .description(
                                                                            "The name of the stage of the deal")
                                                                        .required(false))
                                                                .required(false),
                                                            object("person").properties(integer(
                                                                "id").description(
                                                                    "The ID of the person the deal is associated with")
                                                                    .required(false),
                                                                string(
                                                                    "name").description(
                                                                        "The name of the person the deal is associated with")
                                                                        .required(false))
                                                                .required(false),
                                                            object("organization").properties(integer("id").description(
                                                                "The ID of the organization the deal is associated with")
                                                                .required(false),
                                                                string("name").description(
                                                                    "The name of the organization the deal is associated with")
                                                                    .required(false))
                                                                .required(false),
                                                            array("custom_fields")
                                                                .items(string().description("Custom fields"))
                                                                .description("Custom fields")
                                                                .required(false),
                                                            array("notes")
                                                                .items(string().description("An array of notes"))
                                                                .description("An array of notes")
                                                                .required(false))
                                                        .required(false))
                                                .description("The array of deals"))
                                        .description("The array of deals")
                                        .required(false))
                            .required(false),
                    bool("success").description("If the response is successful or not")
                        .required(false),
                    object("additional_data")
                        .properties(object("pagination").properties(integer("start").description("Pagination start")
                            .required(false),
                            integer("limit").description("Items shown per page")
                                .required(false),
                            bool("more_items_in_collection")
                                .description("Whether there are more list items in the collection than displayed")
                                .required(false),
                            integer("next_start").description("Next pagination start")
                                .required(false))
                            .description("Pagination details of the list")
                            .required(false))
                        .required(false))
                .metadata(
                    Map.of(
                        "responseFormat", ResponseFormat.JSON)))
        .sampleOutput(
            "{\"success\":true,\"data\":{\"items\":[{\"result_score\":1.22,\"item\":{\"id\":1,\"type\":\"deal\",\"title\":\"Jane Doe deal\",\"value\":100,\"currency\":\"USD\",\"status\":\"open\",\"visible_to\":3,\"owner\":{\"id\":1},\"stage\":{\"id\":1,\"name\":\"Lead In\"},\"person\":{\"id\":1,\"name\":\"Jane Doe\"},\"organization\":null,\"custom_fields\":[],\"notes\":[]}}]},\"additional_data\":{\"description\":\"The additional data of the list\",\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"integer\",\"description\":\"Pagination start\"},\"limit\":{\"type\":\"integer\",\"description\":\"Items shown per page\"},\"more_items_in_collection\":{\"type\":\"boolean\",\"description\":\"If there are more list items in the collection than displayed or not\"}}}}");
}

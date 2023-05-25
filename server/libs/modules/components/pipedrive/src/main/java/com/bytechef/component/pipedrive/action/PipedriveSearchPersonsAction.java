
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
public class PipedriveSearchPersonsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("searchPersons")
        .title("Search persons")
        .description(
            "Searches all persons by name, email, phone, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope. Found persons can be filtered by organization ID.")
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
            integer("organization_id").label("Organization Id")
                .description(
                    "Will filter persons by the provided organization ID. The upper limit of found persons associated with the organization is 2000.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("include_fields").label("Include Fields")
                .description("Supports including optional fields in the results which are not provided by default")
                .options(option("Person.picture", "person.picture"))
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
                                array("items")
                                    .items(
                                        object()
                                            .properties(number("result_score").description("Search result relevancy")
                                                .required(false),
                                                object("item")
                                                    .properties(integer("id").description("The ID of the person")
                                                        .required(false),
                                                        string("type").description("The type of the item")
                                                            .required(false),
                                                        string("name").description("The name of the person")
                                                            .required(false),
                                                        array("phones")
                                                            .items(string().description("An array of phone numbers"))
                                                            .description("An array of phone numbers")
                                                            .required(false),
                                                        array("emails")
                                                            .items(string().description("An array of email addresses"))
                                                            .description("An array of email addresses")
                                                            .required(false),
                                                        integer("visible_to")
                                                            .description("The visibility of the person")
                                                            .required(false),
                                                        object("owner").properties(integer("id")
                                                            .description("The ID of the owner of the person")
                                                            .required(false))
                                                            .required(false),
                                                        object("organization").properties(integer(
                                                            "id").description(
                                                                "The ID of the organization the person is associated with")
                                                                .required(false),
                                                            string("name").description(
                                                                "The name of the organization the person is associated with")
                                                                .required(false))
                                                            .required(false),
                                                        array("custom_fields")
                                                            .items(string().description("Custom fields"))
                                                            .description("Custom fields")
                                                            .required(false),
                                                        array("notes").items(string().description("An array of notes"))
                                                            .description("An array of notes")
                                                            .required(false))
                                                    .required(false))
                                            .description("The array of found items"))
                                    .description("The array of found items")
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
            "{\"success\":true,\"data\":{\"items\":[{\"result_score\":0.5092,\"item\":{\"id\":1,\"type\":\"person\",\"name\":\"Jane Doe\",\"phones\":[\"+372 555555555\"],\"emails\":[\"jane@pipedrive.com\"],\"visible_to\":3,\"owner\":{\"id\":1},\"organization\":{\"id\":1,\"name\":\"Organization name\",\"address\":null},\"custom_fields\":[],\"notes\":[]}}]},\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}");
}

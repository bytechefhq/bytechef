
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

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ItemSearchActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(action("searchItem")
        .display(
            display("Perform a search from multiple item types")
                .description("Performs a search from your choice of item types and fields."))
        .metadata(
            Map.of(
                "requestMethod", "GET",
                "path", "/itemSearch"

            ))
        .properties(string("term").label("Term")
            .description(
                "The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.")
            .required(true)
            .metadata(
                Map.of(
                    "type", "QUERY")),
            string("item_types").label("Item_types")
                .description(
                    "A comma-separated string array. The type of items to perform the search from. Defaults to all.")
                .options(option("Deal", "deal"), option("Person", "person"), option("Organization", "organization"),
                    option("Product", "product"), option("Lead", "lead"), option("File", "file"),
                    option("Mail_attachment", "mail_attachment"), option("Project", "project"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
            string("fields").label("Fields")
                .description(
                    "A comma-separated string array. The fields to perform the search from. Defaults to all. Relevant for each item type are:<br> <table> <tr><th><b>Item type</b></th><th><b>Field</b></th></tr> <tr><td>Deal</td><td>`custom_fields`, `notes`, `title`</td></tr> <tr><td>Person</td><td>`custom_fields`, `email`, `name`, `notes`, `phone`</td></tr> <tr><td>Organization</td><td>`address`, `custom_fields`, `name`, `notes`</td></tr> <tr><td>Product</td><td>`code`, `custom_fields`, `name`</td></tr> <tr><td>Lead</td><td>`custom_fields`, `notes`, `email`, `organization_name`, `person_name`, `phone`, `title`</td></tr> <tr><td>File</td><td>`name`</td></tr> <tr><td>Mail attachment</td><td>`name`</td></tr> <tr><td>Project</td><td> `custom_fields`, `notes`, `title`, `description` </td></tr> </table> <br> When searching for leads, the email, organization_name, person_name, and phone fields will return results only for leads not linked to contacts. For searching leads by person or organization values, please use `search_for_related_items`.")
                .options(option("Address", "address"), option("Code", "code"), option("Custom_fields", "custom_fields"),
                    option("Email", "email"), option("Name", "name"), option("Notes", "notes"),
                    option("Organization_name", "organization_name"), option("Person_name", "person_name"),
                    option("Phone", "phone"), option("Title", "title"), option("Description", "description"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
            bool("search_for_related_items").label("Search_for_related_items")
                .description(
                    "When enabled, the response will include up to 100 newest related leads and 100 newest related deals for each found person and organization and up to 100 newest related persons for each found organization.")
                .options(option("True", true), option("False", false))
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
            bool("exact_match").label("Exact_match")
                .description(
                    "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                .options(option("True", true), option("False", false))
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
            string("include_fields").label("Include_fields")
                .description(
                    "A comma-separated string array. Supports including optional fields in the results which are not provided by default.")
                .options(option("Deal.cc_email", "deal.cc_email"), option("Person.picture", "person.picture"),
                    option("Product.price", "product.price"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
            integer("start").label("Start")
                .description(
                    "Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
            integer("limit").label("Limit")
                .description("Items shown per page")
                .required(false)
                .metadata(
                    Map.of(
                        "type", "QUERY")))
        .output(object(null)
            .properties(
                object("data")
                    .properties(
                        array("items").items(object(null).properties(number("result_score").label("Result_score")
                            .description("Search result relevancy")
                            .required(false),
                            object("item").label("Item")
                                .description("Item")
                                .required(false))
                            .description("The array of found items"))
                            .placeholder("Add")
                            .label("Items")
                            .description("The array of found items")
                            .required(false),
                        array("related_items")
                            .items(object(null).properties(number("result_score").label("Result_score")
                                .description("Search result relevancy")
                                .required(false),
                                object("item").label("Item")
                                    .description("Item")
                                    .required(false))
                                .description("The array of related items if `search_for_related_items` was enabled"))
                            .placeholder("Add")
                            .label("Related_items")
                            .description("The array of related items if `search_for_related_items` was enabled")
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
                    "responseType", "JSON")))
        .exampleOutput(
            "{\"success\":true,\"data\":{\"items\":[{\"result_score\":1.22724,\"item\":{\"id\":42,\"type\":\"deal\",\"title\":\"Sample Deal\",\"value\":53883,\"currency\":\"USD\",\"status\":\"open\",\"visible_to\":3,\"owner\":{\"id\":69},\"stage\":{\"id\":3,\"name\":\"Demo Scheduled\"},\"person\":{\"id\":6,\"name\":\"Sample Person\"},\"organization\":{\"id\":9,\"name\":\"Sample Organization\",\"address\":\"Dabas, Hungary\"},\"custom_fields\":[\"Sample text\"],\"notes\":[\"Sample note\"]}},{\"result_score\":0.31335002,\"item\":{\"id\":9,\"type\":\"organization\",\"name\":\"Sample Organization\",\"address\":\"Dabas, Hungary\",\"visible_to\":3,\"owner\":{\"id\":69},\"custom_fields\":[],\"notes\":[]}},{\"result_score\":0.29955,\"item\":{\"id\":6,\"type\":\"person\",\"name\":\"Sample Person\",\"phones\":[\"555123123\",\"+372 (55) 123468\",\"0231632772\"],\"emails\":[\"primary@email.com\",\"secondary@email.com\"],\"visible_to\":1,\"owner\":{\"id\":69},\"organization\":{\"id\":9,\"name\":\"Sample Organization\",\"address\":\"Dabas, Hungary\"},\"custom_fields\":[\"Custom Field Text\"],\"notes\":[\"Person note\"]}},{\"result_score\":0.0093,\"item\":{\"id\":4,\"type\":\"mail_attachment\",\"name\":\"Sample mail attachment.txt\",\"url\":\"/files/4/download\"}},{\"result_score\":0.0093,\"item\":{\"id\":3,\"type\":\"file\",\"name\":\"Sample file attachment.txt\",\"url\":\"/files/3/download\",\"deal\":{\"id\":42,\"title\":\"Sample Deal\"},\"person\":{\"id\":6,\"name\":\"Sample Person\"},\"organization\":{\"id\":9,\"name\":\"Sample Organization\",\"address\":\"Dabas, Hungary\"}}},{\"result_score\":0.0011999999,\"item\":{\"id\":1,\"type\":\"product\",\"name\":\"Sample Product\",\"code\":\"product-code\",\"visible_to\":3,\"owner\":{\"id\":69},\"custom_fields\":[]}}],\"related_items\":[{\"result_score\":0,\"item\":{\"id\":2,\"type\":\"deal\",\"title\":\"Other deal\",\"value\":100,\"currency\":\"USD\",\"status\":\"open\",\"visible_to\":3,\"owner\":{\"id\":1},\"stage\":{\"id\":1,\"name\":\"Lead In\"},\"person\":{\"id\":1,\"name\":\"Sample Person\"}}}]},\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}"),
        action("searchItemByField")
            .display(
                display("Perform a search using a specific field from an item type")
                    .description(
                        "Performs a search from the values of a specific field. Results can either be the distinct values of the field (useful for searching autocomplete field values), or the IDs of actual items (deals, leads, persons, organizations or products)."))
            .metadata(
                Map.of(
                    "requestMethod", "GET",
                    "path", "/itemSearch/field"

                ))
            .properties(string("term").label("Term")
                .description(
                    "The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", "QUERY")),
                string("field_type").label("Field_type")
                    .description("The type of the field to perform the search from")
                    .options(option("DealField", "dealField"), option("LeadField", "leadField"),
                        option("PersonField", "personField"), option("OrganizationField", "organizationField"),
                        option("ProductField", "productField"), option("ProjectField", "projectField"))
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                bool("exact_match").label("Exact_match")
                    .description(
                        "When enabled, only full exact matches against the given term are returned. The search <b>is</b> case sensitive.")
                    .options(option("True", true), option("False", false))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                string("field_key").label("Field_key")
                    .description(
                        "The key of the field to search from. The field key can be obtained by fetching the list of the fields using any of the fields' API GET methods (dealFields, personFields, etc.).")
                    .required(true)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                bool("return_item_ids").label("Return_item_ids")
                    .description(
                        "Whether to return the IDs of the matching items or not. When not set or set to `0` or `false`, only distinct values of the searched field are returned. When set to `1` or `true`, the ID of each found item is returned.")
                    .options(option("True", true), option("False", false))
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("start").label("Start")
                    .description("Pagination start")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")),
                integer("limit").label("Limit")
                    .description("Items shown per page")
                    .required(false)
                    .metadata(
                        Map.of(
                            "type", "QUERY")))
            .output(object(null).properties(array("data").items(object(null).properties(integer("id").label("Id")
                .description("The ID of the item")
                .required(false),
                object("$field_key").label("$field_key")
                    .description("The value of the searched `field_key`")
                    .required(false))
                .description("The array of results"))
                .placeholder("Add")
                .label("Data")
                .description("The array of results")
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
                        "responseType", "TEXT")))
            .exampleOutput(
                "{\"success\":true,\"data\":[{\"id\":1,\"name\":\"Jane Doe\"},{\"id\":2,\"name\":\"John Doe\"}],\"additional_data\":{\"pagination\":{\"start\":0,\"limit\":100,\"more_items_in_collection\":false}}}"));
}

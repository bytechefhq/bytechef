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
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveGetLeadsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getLeads")
        .title("Get all leads")
        .description(
            "Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest. Pagination can be controlled using `limit` and `start` query parameters. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields' structure from deals.\n")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/leads"

            ))
        .properties(integer("limit").label("Limit")
            .description(
                "For pagination, the limit of entries to be returned. If not provided, 100 items will be returned.")
            .required(false)
            .exampleValue(100)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            integer("start").label("Start")
                .description("For pagination, the position that represents the first result for the page")
                .required(false)
                .exampleValue(0)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("archived_status").label("Archived Status")
                .description("Filtering based on the archived status of a lead. If not provided, `All` is used.")
                .options(option("Archived", "archived"), option("Not_archived", "not_archived"), option("All", "all"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("owner_id").label("Owner Id")
                .description(
                    "If supplied, only leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied.")
                .required(false)
                .exampleValue(1)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("person_id").label("Person Id")
                .description(
                    "If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied.")
                .required(false)
                .exampleValue(1)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization Id")
                .description(
                    "If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied.")
                .required(false)
                .exampleValue(1)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("filter_id").label("Filter Id")
                .description("The ID of the filter to use")
                .required(false)
                .exampleValue(1)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("sort").label("Sort")
                .description(
                    "The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).")
                .options(option("Id", "id"), option("Title", "title"), option("Owner_id", "owner_id"),
                    option("Creator_id", "creator_id"), option("Was_seen", "was_seen"),
                    option("Expected_close_date", "expected_close_date"),
                    option("Next_activity_id", "next_activity_id"), option("Add_time", "add_time"),
                    option("Update_time", "update_time"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(object()
            .properties(bool("success").required(false), array("data").items(object().properties(
                string("id").description("The unique ID of the lead in the UUID format")
                    .required(false),
                string("title").description("The title of the lead")
                    .required(false),
                integer("owner_id").description("The ID of the user who owns the lead")
                    .required(false),
                integer("creator_id").description("The ID of the user who created the lead")
                    .required(false),
                array("label_ids")
                    .items(string().description("The IDs of the lead labels which are associated with the lead"))
                    .description("The IDs of the lead labels which are associated with the lead")
                    .required(false),
                integer("person_id").description("The ID of a person which this lead is linked to")
                    .required(false),
                integer("organization_id").description("The ID of an organization which this lead is linked to")
                    .required(false),
                string("source_name").description(
                    "Defines where the lead comes from. Will be `API` if the lead was created through the Public API and will be `Manually created` if the lead was created manually through the UI.\n")
                    .required(false),
                bool("is_archived").description("A flag indicating whether the lead is archived or not")
                    .required(false),
                bool("was_seen")
                    .description("A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                    .required(false),
                object("value").properties(number("amount").required(true), string("currency").required(true))
                    .description("The potential value of the lead")
                    .required(false),
                date("expected_close_date").description(
                    "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                    .required(false),
                integer("next_activity_id").description("The ID of the next activity associated with the lead")
                    .required(false),
                dateTime("add_time")
                    .description(
                        "The date and time of when the lead was created. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                    .required(false),
                dateTime("update_time").description(
                    "The date and time of when the lead was last updated. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                    .required(false),
                string("visible_to").description(
                    "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers (private)</td></tr><tr><td>`3`</td><td>Entire company (shared)</td></tr></table>")
                    .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                    .required(false),
                string("cc_email").description("The BCC email of the lead")
                    .required(false)))
                .required(false),
                object("additional_data").properties(integer("start").description("Pagination start")
                    .required(false),
                    integer("limit").description("Items shown per page")
                        .required(false),
                    bool("more_items_in_collection")
                        .description("If there are more list items in the collection than displayed or not")
                        .required(false))
                    .description("The additional data of the list")
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)))
        .sampleOutput(Map.<String, Object>ofEntries(Map.entry("success", true),
            Map.entry("data",
                List.of(Map.<String, Object>ofEntries(Map.entry("id", "adf21080-0e10-11eb-879b-05d71fb426ec"),
                    Map.entry("title", "Jane Doe Lead"), Map.entry("owner_id", 1), Map.entry("creator_id", 1),
                    Map.entry("label_ids",
                        List.of("f08b42a0-4e75-11ea-9643-03698ef1cfd6", "f08b42a1-4e75-11ea-9643-03698ef1cfd6")),
                    Map.entry("person_id", 1092), Map.entry("organization_id", ""), Map.entry("source_name", "API"),
                    Map.entry("is_archived", false), Map.entry("was_seen", false),
                    Map.entry("value",
                        Map.<String, Object>ofEntries(Map.entry("amount", 999), Map.entry("currency", "USD"))),
                    Map.entry("expected_close_date", ""), Map.entry("next_activity_id", 1),
                    Map.entry("add_time", LocalDateTime.of(2020, 10, 14, 11, 30, 36)),
                    Map.entry("update_time", LocalDateTime.of(2020, 10, 14, 11, 30, 36)), Map.entry("visible_to", 3.0),
                    Map.entry("cc_email", "company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com"))))));
}

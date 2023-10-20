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
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.List;
import java.util.Map;

public class LeadsActions {
    public static final List<ComponentDSL.ModifiableActionDefinition> ACTIONS = List.of(
            action("getLeads")
                    .display(
                            display("Get all leads")
                                    .description(
                                            "Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest. Pagination can be controlled using `limit` and `start` query parameters. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields' structure from deals.\n"))
                    .metadata(Map.of("requestMethod", "GET", "path", "/leads"))
                    .properties(
                            integer("limit")
                                    .label("Limit")
                                    .description(
                                            "For pagination, the limit of entries to be returned. If not provided, 100 items will be returned.")
                                    .required(false)
                                    .exampleValue(100)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("start")
                                    .label("Start")
                                    .description(
                                            "For pagination, the position that represents the first result for the page")
                                    .required(false)
                                    .exampleValue(0)
                                    .metadata(Map.of("type", "QUERY")),
                            string("archived_status")
                                    .label("Archived_status")
                                    .description(
                                            "Filtering based on the archived status of a lead. If not provided, `All` is used.")
                                    .options(
                                            option("Archived", "archived"),
                                            option("Not_archived", "not_archived"),
                                            option("All", "all"))
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("owner_id")
                                    .label("Owner_id")
                                    .description(
                                            "If supplied, only leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied.")
                                    .required(false)
                                    .exampleValue(1)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("person_id")
                                    .label("Person_id")
                                    .description(
                                            "If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied.")
                                    .required(false)
                                    .exampleValue(1)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("organization_id")
                                    .label("Organization_id")
                                    .description(
                                            "If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied.")
                                    .required(false)
                                    .exampleValue(1)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("filter_id")
                                    .label("Filter_id")
                                    .description("The ID of the filter to use")
                                    .required(false)
                                    .exampleValue(1)
                                    .metadata(Map.of("type", "QUERY")),
                            string("sort")
                                    .label("Sort")
                                    .description(
                                            "The field names and sorting mode separated by a comma (`field_name_1 ASC`, `field_name_2 DESC`). Only first-level field keys are supported (no nested keys).")
                                    .options(
                                            option("Id", "id"),
                                            option("Title", "title"),
                                            option("Owner_id", "owner_id"),
                                            option("Creator_id", "creator_id"),
                                            option("Was_seen", "was_seen"),
                                            option("Expected_close_date", "expected_close_date"),
                                            option("Next_activity_id", "next_activity_id"),
                                            option("Add_time", "add_time"),
                                            option("Update_time", "update_time"))
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")))
                    .output(object(null)
                            .properties(
                                    bool("success").label("Success").required(false),
                                    array("data")
                                            .items(object(null)
                                                    .properties(
                                                            string("id")
                                                                    .label("Id")
                                                                    .description(
                                                                            "The unique ID of the lead in the UUID format")
                                                                    .required(false),
                                                            string("title")
                                                                    .label("Title")
                                                                    .description("The title of the lead")
                                                                    .required(false),
                                                            integer("owner_id")
                                                                    .label("Owner_id")
                                                                    .description("The ID of the user who owns the lead")
                                                                    .required(false),
                                                            integer("creator_id")
                                                                    .label("Creator_id")
                                                                    .description(
                                                                            "The ID of the user who created the lead")
                                                                    .required(false),
                                                            array("label_ids")
                                                                    .items(
                                                                            string(null)
                                                                                    .description(
                                                                                            "The IDs of the lead labels which are associated with the lead"))
                                                                    .label("Label_ids")
                                                                    .description(
                                                                            "The IDs of the lead labels which are associated with the lead")
                                                                    .required(false),
                                                            integer("person_id")
                                                                    .label("Person_id")
                                                                    .description(
                                                                            "The ID of a person which this lead is linked to")
                                                                    .required(false),
                                                            integer("organization_id")
                                                                    .label("Organization_id")
                                                                    .description(
                                                                            "The ID of an organization which this lead is linked to")
                                                                    .required(false),
                                                            string("source_name")
                                                                    .label("Source_name")
                                                                    .description(
                                                                            "Defines where the lead comes from. Will be `API` if the lead was created through the Public API and will be `Manually created` if the lead was created manually through the UI.\n")
                                                                    .required(false),
                                                            bool("is_archived")
                                                                    .label("Is_archived")
                                                                    .description(
                                                                            "A flag indicating whether the lead is archived or not")
                                                                    .required(false),
                                                            bool("was_seen")
                                                                    .label("Was_seen")
                                                                    .description(
                                                                            "A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                                                                    .required(false),
                                                            object("value")
                                                                    .properties(
                                                                            number("amount")
                                                                                    .label("Amount")
                                                                                    .required(true),
                                                                            string("currency")
                                                                                    .label("Currency")
                                                                                    .required(true))
                                                                    .label("Value")
                                                                    .description("The potential value of the lead")
                                                                    .required(false),
                                                            date("expected_close_date")
                                                                    .label("Expected_close_date")
                                                                    .description(
                                                                            "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                                                                    .required(false),
                                                            integer("next_activity_id")
                                                                    .label("Next_activity_id")
                                                                    .description(
                                                                            "The ID of the next activity associated with the lead")
                                                                    .required(false),
                                                            dateTime("add_time")
                                                                    .label("Add_time")
                                                                    .description(
                                                                            "The date and time of when the lead was created. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                                    .required(false),
                                                            dateTime("update_time")
                                                                    .label("Update_time")
                                                                    .description(
                                                                            "The date and time of when the lead was last updated. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                                    .required(false),
                                                            string("visible_to")
                                                                    .label("Visible_to")
                                                                    .description(
                                                                            "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers (private)</td></tr><tr><td>`3`</td><td>Entire company (shared)</td></tr></table>")
                                                                    .options(
                                                                            option("1", "1"),
                                                                            option("3", "3"),
                                                                            option("5", "5"),
                                                                            option("7", "7"))
                                                                    .required(false),
                                                            string("cc_email")
                                                                    .label("Cc_email")
                                                                    .description("The BCC email of the lead")
                                                                    .required(false)))
                                            .label("Data")
                                            .required(false),
                                    object("additional_data")
                                            .properties(
                                                    integer("start")
                                                            .label("Start")
                                                            .description("Pagination start")
                                                            .required(false),
                                                    integer("limit")
                                                            .label("Limit")
                                                            .description("Items shown per page")
                                                            .required(false),
                                                    bool("more_items_in_collection")
                                                            .label("More_items_in_collection")
                                                            .description(
                                                                    "If there are more list items in the collection than displayed or not")
                                                            .required(false))
                                            .label("Additional_data")
                                            .description("The additional data of the list")
                                            .required(false))
                            .metadata(Map.of("responseType", "JSON")))
                    .exampleOutput(
                            "{\"success\":true,\"data\":[{\"id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\",\"title\":\"Jane Doe Lead\",\"owner_id\":1,\"creator_id\":1,\"label_ids\":[\"f08b42a0-4e75-11ea-9643-03698ef1cfd6\",\"f08b42a1-4e75-11ea-9643-03698ef1cfd6\"],\"person_id\":1092,\"organization_id\":null,\"source_name\":\"API\",\"is_archived\":false,\"was_seen\":false,\"value\":{\"amount\":999,\"currency\":\"USD\"},\"expected_close_date\":null,\"next_activity_id\":1,\"add_time\":\"2020-10-14T11:30:36.551Z\",\"update_time\":\"2020-10-14T11:30:36.551Z\",\"visible_to\":\"3\",\"cc_email\":\"company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com\"}]}"),
            action("addLead")
                    .display(
                            display("Add a lead")
                                    .description(
                                            "Creates a lead. A lead always has to be linked to a person or an organization or both. All leads created through the Pipedrive API will have a lead source `API` assigned. Here's the tutorial for <a href=\"https://pipedrive.readme.io/docs/adding-a-lead\" target=\"_blank\" rel=\"noopener noreferrer\">adding a lead</a>. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields' structure from deals. See an example given in the <a href=\"https://pipedrive.readme.io/docs/updating-custom-field-value\" target=\"_blank\" rel=\"noopener noreferrer\">updating custom fields' values tutorial</a>."))
                    .metadata(Map.of("requestMethod", "POST", "path", "/leads", "bodyContentType", "JSON"))
                    .properties(object(null)
                            .properties(
                                    string("title")
                                            .label("Title")
                                            .description("The name of the lead")
                                            .required(true),
                                    integer("owner_id")
                                            .label("Owner_id")
                                            .description(
                                                    "The ID of the user which will be the owner of the created lead. If not provided, the user making the request will be used.")
                                            .required(false),
                                    array("label_ids")
                                            .items(
                                                    string(null)
                                                            .description(
                                                                    "The IDs of the lead labels which will be associated with the lead"))
                                            .label("Label_ids")
                                            .description(
                                                    "The IDs of the lead labels which will be associated with the lead")
                                            .required(false),
                                    integer("person_id")
                                            .label("Person_id")
                                            .description(
                                                    "The ID of a person which this lead will be linked to. If the person does not exist yet, it needs to be created first. This property is required unless `organization_id` is specified.")
                                            .required(false),
                                    integer("organization_id")
                                            .label("Organization_id")
                                            .description(
                                                    "The ID of an organization which this lead will be linked to. If the organization does not exist yet, it needs to be created first. This property is required unless `person_id` is specified.")
                                            .required(false),
                                    object("value")
                                            .properties(
                                                    number("amount")
                                                            .label("Amount")
                                                            .required(true),
                                                    string("currency")
                                                            .label("Currency")
                                                            .required(true))
                                            .label("Value")
                                            .description("The potential value of the lead")
                                            .required(false),
                                    date("expected_close_date")
                                            .label("Expected_close_date")
                                            .description(
                                                    "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                                            .required(false),
                                    string("visible_to")
                                            .label("Visible_to")
                                            .description(
                                                    "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width: 40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width: 40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                                            .options(
                                                    option("1", "1"),
                                                    option("3", "3"),
                                                    option("5", "5"),
                                                    option("7", "7"))
                                            .required(false),
                                    bool("was_seen")
                                            .label("Was_seen")
                                            .description(
                                                    "A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                                            .options(option("True", true), option("False", false))
                                            .required(false))
                            .metadata(Map.of("type", "BODY")))
                    .output(object(null)
                            .properties(
                                    bool("success").label("Success").required(false),
                                    object("data")
                                            .properties(
                                                    string("id")
                                                            .label("Id")
                                                            .description("The unique ID of the lead in the UUID format")
                                                            .required(false),
                                                    string("title")
                                                            .label("Title")
                                                            .description("The title of the lead")
                                                            .required(false),
                                                    integer("owner_id")
                                                            .label("Owner_id")
                                                            .description("The ID of the user who owns the lead")
                                                            .required(false),
                                                    integer("creator_id")
                                                            .label("Creator_id")
                                                            .description("The ID of the user who created the lead")
                                                            .required(false),
                                                    array("label_ids")
                                                            .items(
                                                                    string(null)
                                                                            .description(
                                                                                    "The IDs of the lead labels which are associated with the lead"))
                                                            .label("Label_ids")
                                                            .description(
                                                                    "The IDs of the lead labels which are associated with the lead")
                                                            .required(false),
                                                    integer("person_id")
                                                            .label("Person_id")
                                                            .description(
                                                                    "The ID of a person which this lead is linked to")
                                                            .required(false),
                                                    integer("organization_id")
                                                            .label("Organization_id")
                                                            .description(
                                                                    "The ID of an organization which this lead is linked to")
                                                            .required(false),
                                                    string("source_name")
                                                            .label("Source_name")
                                                            .description(
                                                                    "Defines where the lead comes from. Will be `API` if the lead was created through the Public API and will be `Manually created` if the lead was created manually through the UI.\n")
                                                            .required(false),
                                                    bool("is_archived")
                                                            .label("Is_archived")
                                                            .description(
                                                                    "A flag indicating whether the lead is archived or not")
                                                            .required(false),
                                                    bool("was_seen")
                                                            .label("Was_seen")
                                                            .description(
                                                                    "A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                                                            .required(false),
                                                    object("value")
                                                            .properties(
                                                                    number("amount")
                                                                            .label("Amount")
                                                                            .required(true),
                                                                    string("currency")
                                                                            .label("Currency")
                                                                            .required(true))
                                                            .label("Value")
                                                            .description("The potential value of the lead")
                                                            .required(false),
                                                    date("expected_close_date")
                                                            .label("Expected_close_date")
                                                            .description(
                                                                    "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                                                            .required(false),
                                                    integer("next_activity_id")
                                                            .label("Next_activity_id")
                                                            .description(
                                                                    "The ID of the next activity associated with the lead")
                                                            .required(false),
                                                    dateTime("add_time")
                                                            .label("Add_time")
                                                            .description(
                                                                    "The date and time of when the lead was created. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                            .required(false),
                                                    dateTime("update_time")
                                                            .label("Update_time")
                                                            .description(
                                                                    "The date and time of when the lead was last updated. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                            .required(false),
                                                    string("visible_to")
                                                            .label("Visible_to")
                                                            .description(
                                                                    "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers (private)</td></tr><tr><td>`3`</td><td>Entire company (shared)</td></tr></table>")
                                                            .options(
                                                                    option("1", "1"),
                                                                    option("3", "3"),
                                                                    option("5", "5"),
                                                                    option("7", "7"))
                                                            .required(false),
                                                    string("cc_email")
                                                            .label("Cc_email")
                                                            .description("The BCC email of the lead")
                                                            .required(false))
                                            .label("Data")
                                            .required(false))
                            .metadata(Map.of("responseType", "JSON")))
                    .exampleOutput(
                            "{\"success\":true,\"data\":{\"id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\",\"title\":\"Jane Doe Lead\",\"owner_id\":1,\"creator_id\":1,\"label_ids\":[\"f08b42a0-4e75-11ea-9643-03698ef1cfd6\",\"f08b42a1-4e75-11ea-9643-03698ef1cfd6\"],\"person_id\":1092,\"organization_id\":null,\"source_name\":\"API\",\"is_archived\":false,\"was_seen\":false,\"value\":{\"amount\":999,\"currency\":\"USD\"},\"expected_close_date\":null,\"next_activity_id\":1,\"add_time\":\"2020-10-14T11:30:36.551Z\",\"update_time\":\"2020-10-14T11:30:36.551Z\",\"visible_to\":\"3\",\"cc_email\":\"company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com\"}}"),
            action("deleteLead")
                    .display(display("Delete a lead").description("Deletes a specific lead."))
                    .metadata(Map.of("requestMethod", "DELETE", "path", "/leads/{id}"))
                    .properties(string("id")
                            .label("Id")
                            .description("The ID of the lead")
                            .required(true)
                            .metadata(Map.of("type", "PATH")))
                    .output(object(null)
                            .properties(
                                    bool("success").label("Success").required(false),
                                    object("data")
                                            .properties(string("id").label("Id").required(false))
                                            .label("Data")
                                            .required(false))
                            .metadata(Map.of("responseType", "JSON")))
                    .exampleOutput("{\"success\":true,\"data\":{\"id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\"}}"),
            action("getLead")
                    .display(
                            display("Get one lead")
                                    .description(
                                            "Returns details of a specific lead. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields’ structure from deals."))
                    .metadata(Map.of("requestMethod", "GET", "path", "/leads/{id}"))
                    .properties(string("id")
                            .label("Id")
                            .description("The ID of the lead")
                            .required(true)
                            .metadata(Map.of("type", "PATH")))
                    .output(object(null)
                            .properties(
                                    bool("success").label("Success").required(false),
                                    object("data")
                                            .properties(
                                                    string("id")
                                                            .label("Id")
                                                            .description("The unique ID of the lead in the UUID format")
                                                            .required(false),
                                                    string("title")
                                                            .label("Title")
                                                            .description("The title of the lead")
                                                            .required(false),
                                                    integer("owner_id")
                                                            .label("Owner_id")
                                                            .description("The ID of the user who owns the lead")
                                                            .required(false),
                                                    integer("creator_id")
                                                            .label("Creator_id")
                                                            .description("The ID of the user who created the lead")
                                                            .required(false),
                                                    array("label_ids")
                                                            .items(
                                                                    string(null)
                                                                            .description(
                                                                                    "The IDs of the lead labels which are associated with the lead"))
                                                            .label("Label_ids")
                                                            .description(
                                                                    "The IDs of the lead labels which are associated with the lead")
                                                            .required(false),
                                                    integer("person_id")
                                                            .label("Person_id")
                                                            .description(
                                                                    "The ID of a person which this lead is linked to")
                                                            .required(false),
                                                    integer("organization_id")
                                                            .label("Organization_id")
                                                            .description(
                                                                    "The ID of an organization which this lead is linked to")
                                                            .required(false),
                                                    string("source_name")
                                                            .label("Source_name")
                                                            .description(
                                                                    "Defines where the lead comes from. Will be `API` if the lead was created through the Public API and will be `Manually created` if the lead was created manually through the UI.\n")
                                                            .required(false),
                                                    bool("is_archived")
                                                            .label("Is_archived")
                                                            .description(
                                                                    "A flag indicating whether the lead is archived or not")
                                                            .required(false),
                                                    bool("was_seen")
                                                            .label("Was_seen")
                                                            .description(
                                                                    "A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                                                            .required(false),
                                                    object("value")
                                                            .properties(
                                                                    number("amount")
                                                                            .label("Amount")
                                                                            .required(true),
                                                                    string("currency")
                                                                            .label("Currency")
                                                                            .required(true))
                                                            .label("Value")
                                                            .description("The potential value of the lead")
                                                            .required(false),
                                                    date("expected_close_date")
                                                            .label("Expected_close_date")
                                                            .description(
                                                                    "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                                                            .required(false),
                                                    integer("next_activity_id")
                                                            .label("Next_activity_id")
                                                            .description(
                                                                    "The ID of the next activity associated with the lead")
                                                            .required(false),
                                                    dateTime("add_time")
                                                            .label("Add_time")
                                                            .description(
                                                                    "The date and time of when the lead was created. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                            .required(false),
                                                    dateTime("update_time")
                                                            .label("Update_time")
                                                            .description(
                                                                    "The date and time of when the lead was last updated. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                            .required(false),
                                                    string("visible_to")
                                                            .label("Visible_to")
                                                            .description(
                                                                    "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers (private)</td></tr><tr><td>`3`</td><td>Entire company (shared)</td></tr></table>")
                                                            .options(
                                                                    option("1", "1"),
                                                                    option("3", "3"),
                                                                    option("5", "5"),
                                                                    option("7", "7"))
                                                            .required(false),
                                                    string("cc_email")
                                                            .label("Cc_email")
                                                            .description("The BCC email of the lead")
                                                            .required(false))
                                            .label("Data")
                                            .required(false))
                            .metadata(Map.of("responseType", "JSON")))
                    .exampleOutput(
                            "{\"success\":true,\"data\":{\"id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\",\"title\":\"Jane Doe Lead\",\"owner_id\":1,\"creator_id\":1,\"label_ids\":[\"f08b42a0-4e75-11ea-9643-03698ef1cfd6\",\"f08b42a1-4e75-11ea-9643-03698ef1cfd6\"],\"person_id\":1092,\"organization_id\":null,\"source_name\":\"API\",\"is_archived\":false,\"was_seen\":false,\"value\":{\"amount\":999,\"currency\":\"USD\"},\"expected_close_date\":null,\"next_activity_id\":1,\"add_time\":\"2020-10-14T11:30:36.551Z\",\"update_time\":\"2020-10-14T11:30:36.551Z\",\"visible_to\":\"3\",\"cc_email\":\"company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com\"}}"),
            action("updateLead")
                    .display(
                            display("Update a lead")
                                    .description(
                                            "Updates one or more properties of a lead. Only properties included in the request will be updated. Send `null` to unset a property (applicable for example for `value`, `person_id` or `organization_id`). If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields’ structure from deals. See an example given in the <a href=\"https://pipedrive.readme.io/docs/updating-custom-field-value\" target=\"_blank\" rel=\"noopener noreferrer\">updating custom fields’ values tutorial</a>."))
                    .metadata(Map.of("requestMethod", "PATCH", "path", "/leads/{id}", "bodyContentType", "JSON"))
                    .properties(
                            string("id")
                                    .label("Id")
                                    .description("The ID of the lead")
                                    .required(true)
                                    .metadata(Map.of("type", "PATH")),
                            object(null)
                                    .properties(
                                            string("title")
                                                    .label("Title")
                                                    .description("The name of the lead")
                                                    .required(false),
                                            integer("owner_id")
                                                    .label("Owner_id")
                                                    .description(
                                                            "The ID of the user which will be the owner of the created lead. If not provided, the user making the request will be used.")
                                                    .required(false),
                                            array("label_ids")
                                                    .items(
                                                            string(null)
                                                                    .description(
                                                                            "The IDs of the lead labels which will be associated with the lead"))
                                                    .label("Label_ids")
                                                    .description(
                                                            "The IDs of the lead labels which will be associated with the lead")
                                                    .required(false),
                                            integer("person_id")
                                                    .label("Person_id")
                                                    .description(
                                                            "The ID of a person which this lead will be linked to. If the person does not exist yet, it needs to be created first. A lead always has to be linked to a person or organization or both.\n")
                                                    .required(false),
                                            integer("organization_id")
                                                    .label("Organization_id")
                                                    .description(
                                                            "The ID of an organization which this lead will be linked to. If the organization does not exist yet, it needs to be created first. A lead always has to be linked to a person or organization or both.")
                                                    .required(false),
                                            bool("is_archived")
                                                    .label("Is_archived")
                                                    .description(
                                                            "A flag indicating whether the lead is archived or not")
                                                    .required(false),
                                            object("value")
                                                    .properties(
                                                            number("amount")
                                                                    .label("Amount")
                                                                    .required(true),
                                                            string("currency")
                                                                    .label("Currency")
                                                                    .required(true))
                                                    .label("Value")
                                                    .description("The potential value of the lead")
                                                    .required(false),
                                            date("expected_close_date")
                                                    .label("Expected_close_date")
                                                    .description(
                                                            "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                                                    .required(false),
                                            string("visible_to")
                                                    .label("Visible_to")
                                                    .description(
                                                            "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user. Read more about visibility groups <a href=\"https://support.pipedrive.com/en/article/visibility-groups\" target=\"_blank\" rel=\"noopener noreferrer\">here</a>.<h4>Essential / Advanced plan</h4><table><tr><th style=\"width: 40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers</td><tr><td>`3`</td><td>Entire company</td></tr></table><h4>Professional / Enterprise plan</h4><table><tr><th style=\"width: 40px\">Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner only</td><tr><td>`3`</td><td>Owner's visibility group</td></tr><tr><td>`5`</td><td>Owner's visibility group and sub-groups</td></tr><tr><td>`7`</td><td>Entire company</td></tr></table>")
                                                    .options(
                                                            option("1", "1"),
                                                            option("3", "3"),
                                                            option("5", "5"),
                                                            option("7", "7"))
                                                    .required(false),
                                            bool("was_seen")
                                                    .label("Was_seen")
                                                    .description(
                                                            "A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                                                    .options(option("True", true), option("False", false))
                                                    .required(false))
                                    .metadata(Map.of("type", "BODY")))
                    .output(object(null)
                            .properties(
                                    bool("success").label("Success").required(false),
                                    object("data")
                                            .properties(
                                                    string("id")
                                                            .label("Id")
                                                            .description("The unique ID of the lead in the UUID format")
                                                            .required(false),
                                                    string("title")
                                                            .label("Title")
                                                            .description("The title of the lead")
                                                            .required(false),
                                                    integer("owner_id")
                                                            .label("Owner_id")
                                                            .description("The ID of the user who owns the lead")
                                                            .required(false),
                                                    integer("creator_id")
                                                            .label("Creator_id")
                                                            .description("The ID of the user who created the lead")
                                                            .required(false),
                                                    array("label_ids")
                                                            .items(
                                                                    string(null)
                                                                            .description(
                                                                                    "The IDs of the lead labels which are associated with the lead"))
                                                            .label("Label_ids")
                                                            .description(
                                                                    "The IDs of the lead labels which are associated with the lead")
                                                            .required(false),
                                                    integer("person_id")
                                                            .label("Person_id")
                                                            .description(
                                                                    "The ID of a person which this lead is linked to")
                                                            .required(false),
                                                    integer("organization_id")
                                                            .label("Organization_id")
                                                            .description(
                                                                    "The ID of an organization which this lead is linked to")
                                                            .required(false),
                                                    string("source_name")
                                                            .label("Source_name")
                                                            .description(
                                                                    "Defines where the lead comes from. Will be `API` if the lead was created through the Public API and will be `Manually created` if the lead was created manually through the UI.\n")
                                                            .required(false),
                                                    bool("is_archived")
                                                            .label("Is_archived")
                                                            .description(
                                                                    "A flag indicating whether the lead is archived or not")
                                                            .required(false),
                                                    bool("was_seen")
                                                            .label("Was_seen")
                                                            .description(
                                                                    "A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                                                            .required(false),
                                                    object("value")
                                                            .properties(
                                                                    number("amount")
                                                                            .label("Amount")
                                                                            .required(true),
                                                                    string("currency")
                                                                            .label("Currency")
                                                                            .required(true))
                                                            .label("Value")
                                                            .description("The potential value of the lead")
                                                            .required(false),
                                                    date("expected_close_date")
                                                            .label("Expected_close_date")
                                                            .description(
                                                                    "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                                                            .required(false),
                                                    integer("next_activity_id")
                                                            .label("Next_activity_id")
                                                            .description(
                                                                    "The ID of the next activity associated with the lead")
                                                            .required(false),
                                                    dateTime("add_time")
                                                            .label("Add_time")
                                                            .description(
                                                                    "The date and time of when the lead was created. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                            .required(false),
                                                    dateTime("update_time")
                                                            .label("Update_time")
                                                            .description(
                                                                    "The date and time of when the lead was last updated. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                                                            .required(false),
                                                    string("visible_to")
                                                            .label("Visible_to")
                                                            .description(
                                                                    "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers (private)</td></tr><tr><td>`3`</td><td>Entire company (shared)</td></tr></table>")
                                                            .options(
                                                                    option("1", "1"),
                                                                    option("3", "3"),
                                                                    option("5", "5"),
                                                                    option("7", "7"))
                                                            .required(false),
                                                    string("cc_email")
                                                            .label("Cc_email")
                                                            .description("The BCC email of the lead")
                                                            .required(false))
                                            .label("Data")
                                            .required(false))
                            .metadata(Map.of("responseType", "JSON")))
                    .exampleOutput(
                            "{\"success\":true,\"data\":{\"id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\",\"title\":\"Jane Doe Lead\",\"owner_id\":1,\"creator_id\":1,\"label_ids\":[\"f08b42a0-4e75-11ea-9643-03698ef1cfd6\",\"f08b42a1-4e75-11ea-9643-03698ef1cfd6\"],\"person_id\":1092,\"organization_id\":null,\"source_name\":\"API\",\"is_archived\":false,\"was_seen\":false,\"value\":{\"amount\":999,\"currency\":\"USD\"},\"expected_close_date\":null,\"next_activity_id\":1,\"add_time\":\"2020-10-14T11:30:36.551Z\",\"update_time\":\"2020-10-14T11:30:36.551Z\",\"visible_to\":\"3\",\"cc_email\":\"company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com\"}}"),
            action("searchLeads")
                    .display(
                            display("Search leads")
                                    .description(
                                            "Searches all leads by title, notes and/or custom fields. This endpoint is a wrapper of <a href=\"https://developers.pipedrive.com/docs/api/v1/ItemSearch#searchItem\">/v1/itemSearch</a> with a narrower OAuth scope. Found leads can be filtered by the person ID and the organization ID."))
                    .metadata(Map.of("requestMethod", "GET", "path", "/leads/search"))
                    .properties(
                            string("term")
                                    .label("Term")
                                    .description(
                                            "The search term to look for. Minimum 2 characters (or 1 if using `exact_match`). Please note that the search term has to be URL encoded.")
                                    .required(true)
                                    .metadata(Map.of("type", "QUERY")),
                            string("fields")
                                    .label("Fields")
                                    .description(
                                            "A comma-separated string array. The fields to perform the search from. Defaults to all of them.")
                                    .options(
                                            option("Custom_fields", "custom_fields"),
                                            option("Notes", "notes"),
                                            option("Title", "title"))
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            bool("exact_match")
                                    .label("Exact_match")
                                    .description(
                                            "When enabled, only full exact matches against the given term are returned. It is <b>not</b> case sensitive.")
                                    .options(option("True", true), option("False", false))
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("person_id")
                                    .label("Person_id")
                                    .description(
                                            "Will filter leads by the provided person ID. The upper limit of found leads associated with the person is 2000.")
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("organization_id")
                                    .label("Organization_id")
                                    .description(
                                            "Will filter leads by the provided organization ID. The upper limit of found leads associated with the organization is 2000.")
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            string("include_fields")
                                    .label("Include_fields")
                                    .description(
                                            "Supports including optional fields in the results which are not provided by default")
                                    .options(option("Lead.was_seen", "lead.was_seen"))
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("start")
                                    .label("Start")
                                    .description(
                                            "Pagination start. Note that the pagination is based on main results and does not include related items when using `search_for_related_items` parameter.")
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")),
                            integer("limit")
                                    .label("Limit")
                                    .description("Items shown per page")
                                    .required(false)
                                    .metadata(Map.of("type", "QUERY")))
                    .output(object(null)
                            .properties(
                                    object("data")
                                            .properties(array("items")
                                                    .items(object(null)
                                                            .properties(
                                                                    number("result_score")
                                                                            .label("Result_score")
                                                                            .description("Search result relevancy")
                                                                            .required(false),
                                                                    object("item")
                                                                            .properties(
                                                                                    string("id")
                                                                                            .label("Id")
                                                                                            .description(
                                                                                                    "The ID of the lead")
                                                                                            .required(false),
                                                                                    string("type")
                                                                                            .label("Type")
                                                                                            .description(
                                                                                                    "The type of the item")
                                                                                            .required(false),
                                                                                    string("title")
                                                                                            .label("Title")
                                                                                            .description(
                                                                                                    "The title of the lead")
                                                                                            .required(false),
                                                                                    object("owner")
                                                                                            .properties(
                                                                                                    integer("id")
                                                                                                            .label("Id")
                                                                                                            .description(
                                                                                                                    "The ID of the owner of the lead")
                                                                                                            .required(
                                                                                                                    false))
                                                                                            .label("Owner")
                                                                                            .required(false),
                                                                                    object("person")
                                                                                            .properties(
                                                                                                    integer("id")
                                                                                                            .label("Id")
                                                                                                            .description(
                                                                                                                    "The ID of the person the lead is associated with")
                                                                                                            .required(
                                                                                                                    false),
                                                                                                    string("name")
                                                                                                            .label(
                                                                                                                    "Name")
                                                                                                            .description(
                                                                                                                    "The name of the person the lead is associated with")
                                                                                                            .required(
                                                                                                                    false))
                                                                                            .label("Person")
                                                                                            .required(false),
                                                                                    object("organization")
                                                                                            .properties(
                                                                                                    integer("id")
                                                                                                            .label("Id")
                                                                                                            .description(
                                                                                                                    "The ID of the organization the lead is associated with")
                                                                                                            .required(
                                                                                                                    false),
                                                                                                    string("name")
                                                                                                            .label(
                                                                                                                    "Name")
                                                                                                            .description(
                                                                                                                    "The name of the organization the lead is associated with")
                                                                                                            .required(
                                                                                                                    false))
                                                                                            .label("Organization")
                                                                                            .required(false),
                                                                                    array("phones")
                                                                                            .items(string(null))
                                                                                            .label("Phones")
                                                                                            .required(false),
                                                                                    array("emails")
                                                                                            .items(string(null))
                                                                                            .label("Emails")
                                                                                            .required(false),
                                                                                    array("custom_fields")
                                                                                            .items(
                                                                                                    string(null)
                                                                                                            .description(
                                                                                                                    "Custom fields"))
                                                                                            .label("Custom_fields")
                                                                                            .description(
                                                                                                    "Custom fields")
                                                                                            .required(false),
                                                                                    array("notes")
                                                                                            .items(
                                                                                                    string(null)
                                                                                                            .description(
                                                                                                                    "An array of notes"))
                                                                                            .label("Notes")
                                                                                            .description(
                                                                                                    "An array of notes")
                                                                                            .required(false),
                                                                                    integer("value")
                                                                                            .label("Value")
                                                                                            .description(
                                                                                                    "The value of the lead")
                                                                                            .required(false),
                                                                                    string("currency")
                                                                                            .label("Currency")
                                                                                            .description(
                                                                                                    "The currency of the lead")
                                                                                            .required(false),
                                                                                    integer("visible_to")
                                                                                            .label("Visible_to")
                                                                                            .description(
                                                                                                    "The visibility of the lead")
                                                                                            .required(false),
                                                                                    bool("is_archived")
                                                                                            .label("Is_archived")
                                                                                            .description(
                                                                                                    "A flag indicating whether the lead is archived or not")
                                                                                            .required(false))
                                                                            .label("Item")
                                                                            .required(false))
                                                            .description("The array of leads"))
                                                    .label("Items")
                                                    .description("The array of leads")
                                                    .required(false))
                                            .label("Data")
                                            .required(false),
                                    bool("success")
                                            .label("Success")
                                            .description("If the response is successful or not")
                                            .required(false),
                                    object("additional_data")
                                            .properties(object("pagination")
                                                    .properties(
                                                            integer("start")
                                                                    .label("Start")
                                                                    .description("Pagination start")
                                                                    .required(false),
                                                            integer("limit")
                                                                    .label("Limit")
                                                                    .description("Items shown per page")
                                                                    .required(false),
                                                            bool("more_items_in_collection")
                                                                    .label("More_items_in_collection")
                                                                    .description(
                                                                            "Whether there are more list items in the collection than displayed")
                                                                    .required(false),
                                                            integer("next_start")
                                                                    .label("Next_start")
                                                                    .description("Next pagination start")
                                                                    .required(false))
                                                    .label("Pagination")
                                                    .description("Pagination details of the list")
                                                    .required(false))
                                            .label("Additional_data")
                                            .required(false))
                            .metadata(Map.of("responseType", "JSON")))
                    .exampleOutput(
                            "{\"success\":true,\"data\":{\"items\":[{\"result_score\":0.29,\"item\":{\"id\":\"39c433f0-8a4c-11ec-8728-09968f0a1ca0\",\"type\":\"lead\",\"title\":\"John Doe lead\",\"owner\":{\"id\":1},\"person\":{\"id\":1,\"name\":\"John Doe\"},\"organization\":{\"id\":1,\"name\":\"John company\"},\"phones\":[],\"emails\":[\"john@doe.com\"],\"custom_fields\":[],\"notes\":[],\"value\":100,\"currency\":\"USD\",\"visible_to\":3,\"is_archived\":false}}]},\"additional_data\":{\"description\":\"The additional data of the list\",\"type\":\"object\",\"properties\":{\"start\":{\"type\":\"integer\",\"description\":\"Pagination start\"},\"limit\":{\"type\":\"integer\",\"description\":\"Items shown per page\"},\"more_items_in_collection\":{\"type\":\"boolean\",\"description\":\"If there are more list items in the collection than displayed or not\"}}}}"));
}

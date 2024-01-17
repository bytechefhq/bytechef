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
public class PipedriveGetLeadAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getLead")
        .title("Get one lead")
        .description(
            "Returns details of a specific lead. If a lead contains custom fields, the fields' values will be included in the response in the same format as with the `Deals` endpoints. If a custom field's value hasn't been set for the lead, it won't appear in the response. Please note that leads do not have a separate set of custom fields, instead they inherit the custom fields’ structure from deals.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/leads/{id}"

            ))
        .properties(string("id").label("Id")
            .description("The ID of the lead")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .outputSchema(object()
            .properties(bool("success").required(false), object("data").properties(
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
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)))
        .sampleOutput(Map.<String, Object>ofEntries(Map.entry("success", true),
            Map.entry("data",
                Map.<String, Object>ofEntries(Map.entry("id", "adf21080-0e10-11eb-879b-05d71fb426ec"),
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
                    Map.entry("cc_email", "company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com")))));
}

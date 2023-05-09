
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
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.dateTime;
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
        .outputSchema(object().properties(bool("success").label("Success")
            .required(false),
            object("data").properties(string("id").label("Id")
                .description("The unique ID of the lead in the UUID format")
                .required(false),
                string("title").label("Title")
                    .description("The title of the lead")
                    .required(false),
                integer("owner_id").label("Owner_id")
                    .description("The ID of the user who owns the lead")
                    .required(false),
                integer("creator_id").label("Creator_id")
                    .description("The ID of the user who created the lead")
                    .required(false),
                array("label_ids")
                    .items(string().description("The IDs of the lead labels which are associated with the lead"))
                    .placeholder("Add")
                    .label("Label_ids")
                    .description("The IDs of the lead labels which are associated with the lead")
                    .required(false),
                integer("person_id").label("Person_id")
                    .description("The ID of a person which this lead is linked to")
                    .required(false),
                integer("organization_id").label("Organization_id")
                    .description("The ID of an organization which this lead is linked to")
                    .required(false),
                string("source_name").label("Source_name")
                    .description(
                        "Defines where the lead comes from. Will be `API` if the lead was created through the Public API and will be `Manually created` if the lead was created manually through the UI.\n")
                    .required(false),
                bool("is_archived").label("Is_archived")
                    .description("A flag indicating whether the lead is archived or not")
                    .required(false),
                bool("was_seen").label("Was_seen")
                    .description("A flag indicating whether the lead was seen by someone in the Pipedrive UI")
                    .required(false),
                object("value").properties(number("amount").label("Amount")
                    .required(true),
                    string("currency").label("Currency")
                        .required(true))
                    .label("Value")
                    .description("The potential value of the lead")
                    .required(false),
                date("expected_close_date").label("Expected_close_date")
                    .description(
                        "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                    .required(false),
                integer("next_activity_id").label("Next_activity_id")
                    .description("The ID of the next activity associated with the lead")
                    .required(false),
                dateTime("add_time").label("Add_time")
                    .description(
                        "The date and time of when the lead was created. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                    .required(false),
                dateTime("update_time").label("Update_time")
                    .description(
                        "The date and time of when the lead was last updated. In ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.")
                    .required(false),
                string("visible_to").label("Visible_to")
                    .description(
                        "The visibility of the lead. If omitted, the visibility will be set to the default visibility setting of this item type for the authorized user.<table><tr><th>Value</th><th>Description</th></tr><tr><td>`1`</td><td>Owner &amp; followers (private)</td></tr><tr><td>`3`</td><td>Entire company (shared)</td></tr></table>")
                    .options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                    .required(false),
                string("cc_email").label("Cc_email")
                    .description("The BCC email of the lead")
                    .required(false))
                .label("Data")
                .required(false))
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .sampleOutput(
            "{\"success\":true,\"data\":{\"id\":\"adf21080-0e10-11eb-879b-05d71fb426ec\",\"title\":\"Jane Doe Lead\",\"owner_id\":1,\"creator_id\":1,\"label_ids\":[\"f08b42a0-4e75-11ea-9643-03698ef1cfd6\",\"f08b42a1-4e75-11ea-9643-03698ef1cfd6\"],\"person_id\":1092,\"organization_id\":null,\"source_name\":\"API\",\"is_archived\":false,\"was_seen\":false,\"value\":{\"amount\":999,\"currency\":\"USD\"},\"expected_close_date\":null,\"next_activity_id\":1,\"add_time\":\"2020-10-14T11:30:36.551Z\",\"update_time\":\"2020-10-14T11:30:36.551Z\",\"visible_to\":\"3\",\"cc_email\":\"company+1+leadntPaYKA5QRxXkh6WMNHiGh@dev.pipedrivemail.com\"}}");
}

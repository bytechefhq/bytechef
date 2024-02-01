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
        .outputSchema(bool("success").required(false),
            object("data")
                .properties(string("id").required(false), string("title").required(false),
                    integer("owner_id").required(false), integer("creator_id").required(false),
                    array("label_ids").items(string())
                        .required(false),
                    integer("person_id").required(false), integer("organization_id").required(false),
                    string("source_name").required(false), bool("is_archived").required(false),
                    bool("was_seen").required(false),
                    object("value").properties(number("amount").required(true), string("currency").required(true))
                        .required(false),
                    date("expected_close_date").required(false), integer("next_activity_id").required(false),
                    dateTime("add_time").required(false), dateTime("update_time").required(false),
                    string("visible_to").options(option("1", "1"), option("3", "3"), option("5", "5"), option("7", "7"))
                        .required(false),
                    string("cc_email").required(false))
                .required(false))
        .outputSchemaMetadata(Map.of(
            "responseType", ResponseType.JSON))
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

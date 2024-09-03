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

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveGetLeadsAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getLeads")
        .title("Get leads")
        .description("Returns multiple leads. Leads are sorted by the time they were created, from oldest to newest.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/leads"

            ))
        .properties(string("archived_status").label("Archived Status")
            .description("Filtering based on the archived status of a lead.")
            .options(option("Archived", "archived"), option("Not_archived", "not_archived"), option("All", "all"))
            .defaultValue("all")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            integer("owner_id").label("Owner")
                .description(
                    "Leads matching the given user will be returned. However, `filter_id` takes precedence over `owner_id` when supplied.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("person_id").label("Person")
                .description(
                    "If supplied, only leads matching the given person will be returned. However, `filter_id` takes precedence over `person_id` when supplied.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("organization_id").label("Organization")
                .description(
                    "If supplied, only leads matching the given organization will be returned. However, `filter_id` takes precedence over `organization_id` when supplied.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("filter_id").label("Filter")
                .description("Filter to use")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("sort").label("Sort")
                .description(
                    "The field names and sorting mode separated by a comma. Only first-level field keys are supported (no nested keys).")
                .options(option("Id", "id"), option("Title", "title"), option("Owner_id", "owner_id"),
                    option("Creator_id", "creator_id"), option("Was_seen", "was_seen"),
                    option("Expected_close_date", "expected_close_date"),
                    option("Next_activity_id", "next_activity_id"), option("Add_time", "add_time"),
                    option("Update_time", "update_time"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(
            object()
                .properties(
                    object("body")
                        .properties(
                            array("data")
                                .items(object().properties(string("id").required(false),
                                    string("title").required(false), integer("owner_id").required(false),
                                    object("value")
                                        .properties(integer("amount").required(false),
                                            string("currency").required(false))
                                        .required(false),
                                    date("expected_close_date").required(false), integer("person_id").required(false)))
                                .required(false))
                        .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON)));

    private PipedriveGetLeadsAction() {
    }
}

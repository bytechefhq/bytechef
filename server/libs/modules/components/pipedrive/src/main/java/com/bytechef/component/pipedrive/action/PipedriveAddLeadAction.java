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
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveAddLeadAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addLead")
        .title("Add Lead")
        .description("Creates a lead. A lead always has to be linked to a person or an organization or both.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/leads", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("title").label("Title")
            .description("The name of the lead.")
            .required(true),
            integer("owner_id").label("Owner")
                .description("User which will be the owner of the created lead.")
                .required(false),
            array("label_ids").items(string().description("Lead labels which will be associated with the lead."))
                .placeholder("Add to Label Ids")
                .label("Lead   Labels")
                .description("Lead labels which will be associated with the lead.")
                .required(false),
            integer("person_id").label("Person")
                .description("Person which this lead will be linked to.")
                .required(false),
            integer("organization_id").label("Organization")
                .description("Organization which this lead will be linked to.")
                .required(false),
            object("value").properties(number("amount").label("Amount")
                .required(true),
                string("currency").label("Currency")
                    .required(true))
                .label("Value")
                .description("The potential value of the lead")
                .required(false),
            date("expected_close_date").label("Expected Close Date")
                .description(
                    "The date of when the deal which will be created from the lead is expected to be closed. In ISO 8601 format: YYYY-MM-DD.")
                .required(false))
            .label("Lead")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("body")
                            .properties(object("data")
                                .properties(string("id").required(false), string("title").required(false),
                                    integer("owner_id").required(false),
                                    object("value")
                                        .properties(integer("amount").required(false),
                                            string("currency").required(false))
                                        .required(false),
                                    date("expected_close_date").required(false), integer("person_id").required(false))
                                .required(false))
                            .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))));

    private PipedriveAddLeadAction() {
    }
}

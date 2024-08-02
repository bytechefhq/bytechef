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
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipedriveAddDealAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("addDeal")
        .title("Add deal")
        .description("Adds a new deal.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/deals", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("title").label("Title")
            .description("The title of the deal")
            .required(true),
            string("value").label("Value")
                .description("The value of the deal.")
                .defaultValue("0")
                .required(false),
            string("currency").label("Currency")
                .description(
                    "The currency of the deal. If omitted, currency will be set to the default currency of the authorized user.")
                .required(false),
            integer("user_id").label("User")
                .description(
                    "User which will be the owner of the  created deal. If not provided, the user making the request will be used.")
                .required(false),
            integer("person_id").label("Person")
                .description(
                    "Person which this deal will be linked to. This property is required unless `org_id` is specified.")
                .required(false),
            integer("org_id").label("Organization")
                .description(
                    "Organization which this deal will be linked to. This property is required unless `person_id` is specified.")
                .required(false),
            integer("pipeline_id").label("Pipeline")
                .description(
                    "Pipeline this deal will be added to. By default, the deal will be added to the first stage of the specified pipeline. Please note that `pipeline_id` and `stage_id` should not be used together as `pipeline_id` will be ignored.")
                .required(false),
            integer("stage_id").label("Stage")
                .description(
                    "Stage this deal will be added to. Please note that a pipeline will be assigned automatically based on the `stage_id`. If omitted, the deal will be placed in the first stage of the default pipeline.")
                .required(false),
            string("status").label("Status")
                .options(option("Open", "open"), option("Won", "won"), option("Lost", "lost"),
                    option("Deleted", "deleted"))
                .defaultValue("open")
                .required(false),
            date("expected_close_date").label("Expected Close Date")
                .description("The expected close date of the deal.")
                .required(false),
            number("probability").label("Probability")
                .description(
                    "The success probability percentage of the deal. Used/shown only when `deal_probability` for the pipeline of the deal is enabled.")
                .required(false),
            string("lost_reason").label("Lost Reason")
                .description("The optional message about why the deal was lost.")
                .required(false))
            .label("Deal")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(object("data")
                    .properties(
                        integer("id").required(false), object("user_id")
                            .properties(integer("id").required(false), string("name").required(false),
                                string("email").required(false))
                            .required(false),
                        object("person_id").properties(string("name").required(false))
                            .required(false),
                        object("org_id").properties(string("name").required(false), string("owner_id").required(false))
                            .required(false),
                        integer("stage_id").required(false), string("title").required(false),
                        integer("value").required(false), string("currency").required(false),
                        string("status").required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private PipedriveAddDealAction() {
    }
}

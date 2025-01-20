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

package com.bytechef.component.nifty.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class NiftyGetTrackedTimeReportAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getTrackedTimeReport")
        .title("Get Tracked Time Report")
        .description("Gets tracked time report information.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/time"

            ))
        .properties(string("project_id").label("Project ID")
            .description("Id of the project to get the report for.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            dateTime("start_date").label("Start Date")
                .description("Start date for the report.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            dateTime("end_date").label("End Date")
                .description("Start date for the report.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(array("items")
                    .items(object().properties(string("id").required(false), string("project").required(false),
                        string("start").required(false), bool("manual").required(false), string("user").required(false),
                        string("task").required(false), string("end").required(false), bool("active").required(false),
                        string("duration").required(false)))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private NiftyGetTrackedTimeReportAction() {
    }
}

/*
 * Copyright 2025 ByteChef
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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.nifty.util.NiftyUtils;
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
            .options((ActionDefinition.OptionsFunction<String>) NiftyUtils::getProjectIdOptions)
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
            .properties(array("items")
                .items(object().properties(string("id").description("ID of the tracked time entry.")
                    .required(false),
                    string("project").description("ID of the project the tracked time entry belongs to.")
                        .required(false),
                    string("start").description("Start time of the tracked time entry.")
                        .required(false),
                    bool("manual").description("Whether the tracked time entry was manually added.")
                        .required(false),
                    string("user").description("ID of the user that tracked the time.")
                        .required(false),
                    string("task").description("ID of the task the tracked time entry belongs to.")
                        .required(false),
                    string("end").description("End time of the tracked time entry.")
                        .required(false),
                    bool("active").description("Whether the tracked time entry is currently active.")
                        .required(false),
                    string("duration").description("Duration of the tracked time entry.")
                        .required(false)))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private NiftyGetTrackedTimeReportAction() {
    }
}

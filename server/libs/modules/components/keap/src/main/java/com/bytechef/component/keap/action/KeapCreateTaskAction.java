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

package com.bytechef.component.keap.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
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
public class KeapCreateTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(bool("completed").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Completed")
            .required(false),
            dateTime("completion_date").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Completion Date")
                .required(false),
            object("contact").properties(string("email").label("Email")
                .required(false),
                string("first_name").label("First Name")
                    .required(false),
                integer("id").label("Id")
                    .required(false),
                string("last_name").label("Last Name")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("BasicContact")
                .required(false),
            dateTime("creation_date").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Creation Date")
                .required(false),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .required(false),
            dateTime("due_date").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Due Date")
                .required(false),
            integer("funnel_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Funnel Id")
                .required(false),
            integer("jgraph_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Jgraph Id")
                .required(false),
            dateTime("modification_date").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Modification Date")
                .required(false),
            integer("priority").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Priority")
                .required(false),
            integer("remind_time").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Remind Time")
                .description(
                    "Value in minutes before start_date to show pop-up reminder. Acceptable values are in [`5`,`10`,`15`,`30`,`60`,`120`,`240`,`480`,`1440`,`2880`]")
                .required(false),
            string("title").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Title")
                .required(false),
            string("type").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Type")
                .required(false),
            string("url").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Url")
                .required(false),
            integer("user_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("User Id")
                .required(false))
        .output(
            outputSchema(object()
                .properties(bool("completed").required(false), dateTime("completion_date").required(false),
                    object("contact").properties(
                        string("email").required(false), string("first_name").required(false),
                        integer("id").required(false), string("last_name").required(false))
                        .required(false),
                    dateTime("creation_date").required(false), string("description").required(false),
                    dateTime("due_date").required(false), integer("funnel_id").required(false),
                    integer("jgraph_id").required(false), dateTime("modification_date").required(false),
                    integer("priority").required(false),
                    integer("remind_time").description(
                        "Value in minutes before start_date to show pop-up reminder. Acceptable values are in [`5`,`10`,`15`,`30`,`60`,`120`,`240`,`480`,`1440`,`2880`]")
                        .required(false),
                    string("title").required(false), string("type").required(false), string("url").required(false),
                    integer("user_id").required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON))));

    private KeapCreateTaskAction() {
    }
}

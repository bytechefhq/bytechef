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

package com.bytechef.component.keap.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
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
public class KeapCreateTaskAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create a Task")
        .description("Creates a new task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/v1/tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(bool("completed").label("Completed")
            .required(false),
            dateTime("completion_date").label("Completion Date")
                .required(false),
            object("contact").properties(string("email").label("Email")
                .required(false),
                string("first_name").label("First Name")
                    .required(false),
                integer("id").label("Id")
                    .required(false),
                string("last_name").label("Last Name")
                    .required(false))
                .label("Basic Contact")
                .required(false),
            dateTime("creation_date").label("Creation Date")
                .required(false),
            string("description").label("Description")
                .required(false),
            dateTime("due_date").label("Due Date")
                .required(false),
            integer("funnel_id").label("Funnel Id")
                .required(false),
            integer("jgraph_id").label("Jgraph Id")
                .required(false),
            dateTime("modification_date").label("Modification Date")
                .required(false),
            integer("priority").label("Priority")
                .required(false),
            integer("remind_time").label("Remind Time")
                .description(
                    "Value in minutes before start_date to show pop-up reminder. Acceptable values are in [`5`,`10`,`15`,`30`,`60`,`120`,`240`,`480`,`1440`,`2880`]")
                .required(false),
            string("title").label("Title")
                .required(false),
            string("type").label("Type")
                .required(false),
            string("url").label("Url")
                .required(false),
            integer("user_id").label("User Id")
                .required(false))
            .label("Task")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(
            object()
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
                        "responseType", ResponseType.JSON)));
}

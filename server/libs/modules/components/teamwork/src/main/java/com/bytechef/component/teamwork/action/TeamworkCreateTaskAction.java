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

package com.bytechef.component.teamwork.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.date;
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
public class TeamworkCreateTaskAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Creates a task")
        .description("Create a new task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasklists/{tasklistId}/tasks.json", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(integer("tasklistId").label("Tasklist Id")
            .description("Task list where new task is added")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(object("task").properties(string("name").label("Name")
                .description("Task name")
                .required(false),
                string("description").label("Description")
                    .required(false),
                date("dueAt").label("Due At")
                    .required(false))
                .label("Task")
                .required(false))
                .label("Task")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("name").required(false), string("description").required(false),
                    string("dueAt").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private TeamworkCreateTaskAction() {
    }
}

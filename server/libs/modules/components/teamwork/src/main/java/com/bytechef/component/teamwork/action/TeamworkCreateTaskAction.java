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

package com.bytechef.component.teamwork.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.teamwork.util.TeamworkUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class TeamworkCreateTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Create a new task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasklists/{tasklistId}/tasks.json", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(integer("tasklistId").label("Task List ID")
            .description("Task list where new task is added")
            .required(true)
            .options((ActionDefinition.OptionsFunction<Long>) TeamworkUtils::getTasklistIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("task").properties(string("name").label("Name")
                .description("Task name")
                .required(false),
                string("description").label("Description")
                    .required(false),
                date("dueAt").label("Due Date")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Task")
                .required(false))
        .output(outputSchema(object().properties(string("name").description("Name of the task.")
            .required(false),
            string("description").description("Description of the task.")
                .required(false),
            string("dueAt").description("Due date of the task.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private TeamworkCreateTaskAction() {
    }
}

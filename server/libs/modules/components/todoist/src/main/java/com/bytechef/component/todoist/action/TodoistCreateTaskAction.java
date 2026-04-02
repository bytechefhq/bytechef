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

package com.bytechef.component.todoist.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.todoist.util.TodoistUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class TodoistCreateTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("content").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Content")
            .description("Task content.")
            .required(true),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("A description for the task.")
                .required(false),
            string("project_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Project ID")
                .description("ID of the project to add the task to. If not set, task is put to user's Inbox.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) TodoistUtils::getProjectIdOptions),
            integer("priority").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Priority")
                .description("Task priority from 1 (normal) to 4 (urgent).")
                .options(option("1", 1), option("2", 2), option("3", 3), option("4", 4))
                .required(false),
            array("labels").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("List of labels to be applied to the task."))
                .placeholder("Add to Labels")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Labels")
                .description("List of labels to be applied to the task.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) TodoistUtils::getLabelsOptions),
            string("section_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Section ID")
                .description("ID of the section to add the task to.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) TodoistUtils::getSectionIdOptions)
                .optionsLookupDependsOn("project_id"),
            string("parent_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Parent Task ID")
                .description("ID of the parent task.")
                .required(false))
        .output(outputSchema(object().properties(string("user_id").description("ID of the user.")
            .required(false),
            string("id").description("ID of the task.")
                .required(false),
            string("project_id").description("ID of the project.")
                .required(false),
            string("section_id").description("ID of the section.")
                .required(false),
            string("parent_id").description("ID of the parent task.")
                .required(false),
            array("labels").items(string().description("List of labels applied to the task."))
                .description("List of labels applied to the task.")
                .required(false),
            bool("checked").description("Whether the task is checked.")
                .required(false),
            bool("is_deleted").description("Whether the task is deleted.")
                .required(false),
            string("content").description("Task content.")
                .required(false),
            string("description").description("Task description.")
                .required(false),
            integer("priority").description("Task priority.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/todoist_v1#create-task");

    private TodoistCreateTaskAction() {
    }
}

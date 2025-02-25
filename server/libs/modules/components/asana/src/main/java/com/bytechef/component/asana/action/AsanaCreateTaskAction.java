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

package com.bytechef.component.asana.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.asana.util.AsanaUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AsanaCreateTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("data").properties(string("workspace").label("Workspace")
            .description("The workspace to create the task in.")
            .required(true)
            .options((OptionsDataSource.ActionOptionsFunction<String>) AsanaUtils::getWorkspaceOptions),
            string("project").label("Project")
                .description("Asana project to create the task in.")
                .required(true)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AsanaUtils::getProjectOptions),
            string("name").label("Name")
                .description("Name of the task.")
                .required(true),
            string("notes").label("Notes")
                .description("Free-form textual information associated with the task (i.e. its description).")
                .required(true),
            date("due_on").label("Due Date")
                .description("The date on which this task is due.")
                .required(false),
            array("tags").items(string().description("Tags to add to the task."))
                .placeholder("Add to Tags")
                .label("Tags")
                .description("Tags to add to the task.")
                .required(false)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AsanaUtils::getTagsOptions),
            string("assignee").label("Assignee")
                .description("User to assign the task to.")
                .required(false)
                .options((OptionsDataSource.ActionOptionsFunction<String>) AsanaUtils::getAssigneeOptions))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Data")
            .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .properties(string("gid").required(false), date("due_on").required(false),
                                string("notes").required(false), string("name").required(false),
                                object("workspace")
                                    .properties(string("gid").required(false), string("name").required(false))
                                    .required(false),
                                array("tags")
                                    .items(object().properties(string("gid").required(false),
                                        string("name").required(false)))
                                    .required(false),
                                object("assignee")
                                    .properties(string("gid").required(false), string("name").required(false))
                                    .required(false))
                            .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))));

    private AsanaCreateTaskAction() {
    }
}

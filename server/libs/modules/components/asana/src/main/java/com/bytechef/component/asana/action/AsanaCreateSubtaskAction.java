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

package com.bytechef.component.asana.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.asana.util.AsanaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AsanaCreateSubtaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createSubtask")
        .title("Create Subtask")
        .description("Creates a new subtask and adds it to the parent task.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasks/{taskGid}/subtasks", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("taskGid").label("Task gID")
            .description("The task gID of the task that will be the parent of the subtask.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getTaskGidOptions)
            .optionsLookupDependsOn("data.workspace", "data.project")
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("data").properties(string("workspace").label("Workspace")
                .description("The workspace to create the subtask in.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getWorkspaceOptions),
                string("project").label("Project")
                    .description("The project to create the subtask in.")
                    .required(true)
                    .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getProjectOptions)
                    .optionsLookupDependsOn("data.workspace"),
                string("name").label("Name")
                    .description("Name of the subtask.")
                    .required(true),
                string("notes").label("Notes")
                    .description("Free-form textual information associated with the subtask (i.e. its description).")
                    .required(false),
                date("due_on").label("Due Date")
                    .description("The date on which this subtask is due.")
                    .required(false),
                string("assignee").label("Assignee")
                    .description("Gid of a user to assign the subtask to.")
                    .required(false)
                    .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getAssigneeOptions)
                    .optionsLookupDependsOn("data.workspace"))
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
                            .properties(string("gid").description("Globally unique identifier for the subtask.")
                                .required(false),
                                date("due_on").description("The date on which this subtask is due.")
                                    .required(false),
                                string("notes").description(
                                    "Free-form textual information associated with the subtask (i.e. its description).")
                                    .required(false),
                                string("name").description("Name of the subtask.")
                                    .required(false),
                                object("workspace")
                                    .properties(
                                        string("gid").description("Globally unique identifier for the workspace.")
                                            .required(false),
                                        string("name").description("Name of the workspace.")
                                            .required(false))
                                    .description("The workspace or organization that the subtask is associated with.")
                                    .required(false),
                                object("project")
                                    .properties(string("gid").description("Globally unique identifier for the project.")
                                        .required(false),
                                        string("name").description("Name of the project.")
                                            .required(false))
                                    .description("The project that the subtask is associated with.")
                                    .required(false),
                                object("parent")
                                    .properties(
                                        string("gid").description("Globally unique identifier for the parent task.")
                                            .required(false),
                                        string("name").description("Name of the parent task.")
                                            .required(false))
                                    .description("The parent task of this subtask.")
                                    .required(false),
                                object("assignee")
                                    .properties(string("gid").description("Globally unique identifier for the user.")
                                        .required(false),
                                        string("name").description("Name of the user.")
                                            .required(false))
                                    .description("User assigned to the task.")
                                    .required(false))
                            .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/asana_v1#create-subtask");

    private AsanaCreateSubtaskAction() {
    }
}

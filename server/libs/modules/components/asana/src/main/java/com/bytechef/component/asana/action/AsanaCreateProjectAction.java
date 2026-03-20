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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
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
public class AsanaCreateProjectAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create Project")
        .description("Creates a new project in a workspace or team.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/projects", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("data").properties(string("workspace").label("Workspace GID")
            .description("The GID of the workspace to create the project in.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getWorkspaceOptions),
            string("name").label("Name")
                .description("Name of the project.")
                .required(true),
            string("notes").label("Notes")
                .description("Free-form textual information associated with the project (ie., its description).")
                .required(true),
            string("team").label("Team GID")
                .description("The GID of the team that this project is shared with.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) AsanaUtils::getTeamOptions)
                .optionsLookupDependsOn("data.workspace"))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Data")
            .required(false))
        .output(
            outputSchema(object()
                .properties(object("data")
                    .properties(string("gid").description("Globally unique identifier for the project.")
                        .required(false),
                        string("resource_type").description("The base type of this resource.")
                            .required(false),
                        bool("archived").description("Whether the project is archived.")
                            .required(false),
                        string("color").description("The color of the project.")
                            .required(false),
                        string("icon").description("The icon of the project.")
                            .required(false),
                        dateTime("created_at").description("The time at which this project was created.")
                            .required(false),
                        object("current_status_update")
                            .properties(
                                string("gid").description("Globally unique identifier of the resource, as a string.")
                                    .required(false),
                                string("resource_type").description("The base type of this resource.")
                                    .required(false),
                                string("title").description("The title of the status update.")
                                    .required(false),
                                string("resource_subtype").description("The subtype of this resource.")
                                    .required(false))
                            .description("The latest status_update posted to this project.")
                            .required(false),
                        string("default_view").description("The default view of a project.")
                            .required(false),
                        string(
                            "due_on")
                                .description(
                                    "The day on which this project is due. This takes a date with format YYYY-MM-DD.")
                                .required(false),
                        string("html_notes").description("The notes of the project with formatting as HTML.")
                            .required(false),
                        string("name").description("Name of the project.")
                            .required(false),
                        string(
                            "notes")
                                .description(
                                    "Free-form textual information associated with the project (ie., its description).")
                                .required(false),
                        object("team").properties(string("gid").description("Globally unique identifier for the team.")
                            .required(false),
                            string("name").description("Name of the team.")
                                .required(false))
                            .description("The team that this project is shared with.")
                            .required(false),
                        object("workspace")
                            .properties(string("gid").description("Globally unique identifier for the workspace.")
                                .required(false),
                                string("name").description("Name of the workspace.")
                                    .required(false))
                            .description("The workspace or organization that the project is associated with.")
                            .required(false))
                    .required(false))
                .metadata(
                    Map.of(
                        "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/asana_v1#create-project");

    private AsanaCreateProjectAction() {
    }
}

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

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
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
public class AsanaCreateProjectAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create project")
        .description("Creates a new project in a workspace or team.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/projects", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(object("data").properties(string("workspace").label("Workspace")
            .description("The workspace to create the project in.")
            .required(true),
            string("name").label("Name")
                .description("Name of the project.")
                .required(true),
            string("notes").label("Notes")
                .description("Free-form textual information associated with the project (ie., its description).")
                .required(true),
            string("team").label("Team")
                .description("The team that this project is shared with.")
                .required(true))
            .label("Data")
            .required(false))
            .label("Project")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("data")
                .properties(string("gid").required(false), string("name").required(false),
                    string("notes").required(false),
                    object("team").properties(string("gid").required(false), string("name").required(false))
                        .required(false),
                    object("workspace").properties(string("gid").required(false), string("name").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private AsanaCreateProjectAction() {
    }
}

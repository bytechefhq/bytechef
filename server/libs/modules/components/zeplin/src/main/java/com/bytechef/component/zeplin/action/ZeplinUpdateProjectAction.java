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

package com.bytechef.component.zeplin.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ZeplinUpdateProjectAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateProject")
        .title("Update Project")
        .description("Updates an existing project.")
        .metadata(
            Map.of(
                "method", "PATCH",
                "path", "/projects/{project_id}", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("project_id").label("Project ID")
            .description("Project to update.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(string("name").label("Name")
                .description("New name for the project.")
                .required(true),
                string("description").label("Description")
                    .description("New description for the project.")
                    .required(false))
                .label("Project")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)));

    private ZeplinUpdateProjectAction() {
    }
}

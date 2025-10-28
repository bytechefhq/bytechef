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

package com.bytechef.component.bitbucket.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.bitbucket.util.BitbucketUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class BitbucketCreateProjectAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create Project")
        .description("Creates a project in selected workspace.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/workspaces/{workspace}/projects", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("workspace").label("Workspace")
            .description("Workspace where the project will be added.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) BitbucketUtils::getWorkspaceOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("The name of the project.")
                .required(true),
            string("key").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Key")
                .description(
                    "Key of the project (eg. AT, for a project named Atlassian). Project keys must start with a letter and may only consist of ASCII letters, numbers and underscores (A-Z, a-z, 0-9, _).")
                .required(true),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("The description of project.")
                .required(false),
            bool("is_private").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Is Private")
                .description("Whether the project is private or not.")
                .required(false))
        .output(outputSchema(object()
            .properties(object("metrics")
                .properties(integer("org_keywords").description(
                    "The total number of keywords that your target ranks for in the top 100 organic search results.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private BitbucketCreateProjectAction() {
    }
}

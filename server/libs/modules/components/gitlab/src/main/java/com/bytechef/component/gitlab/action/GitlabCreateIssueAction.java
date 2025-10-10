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

package com.bytechef.component.gitlab.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.gitlab.util.GitlabUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GitlabCreateIssueAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createIssue")
        .title("Create Issue")
        .description("Creates a new project issue.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/projects/{projectId}/issues"

            ))
        .properties(string("projectId").label("Project ID")
            .description("ID of the project where new issue will be created.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) GitlabUtils::getProjectIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("title").label("Title")
                .description("The title of an issue.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("description").maxLength(1048576)
                .label("Description")
                .description("The description of an issue.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(integer("id").description("The ID of the issue.")
            .required(false),
            integer("iid").description("The internal ID of the issue.")
                .required(false),
            integer("project_id").description("The ID of the project.")
                .required(false),
            string("title").description("The title of the issue.")
                .required(false),
            string("description").description("The description of the issue.")
                .required(false),
            string("web_url").description("The URL of the issue.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private GitlabCreateIssueAction() {
    }
}

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
public class GitlabCreateCommentOnIssueAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createCommentOnIssue")
        .title("Create Comment on Issue")
        .description("Adds a comment to the specified issue.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/projects/{projectId}/issues/{issueId}/notes"

            ))
        .properties(string("projectId").label("Project ID")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) GitlabUtils::getProjectIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            integer("issueId").label("Issue ID")
                .description("ID of the issue to comment on.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<Long>) GitlabUtils::getIssueIdOptions)
                .optionsLookupDependsOn("projectId")
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            string("body").maxLength(1000000)
                .label("Comment")
                .description("The comment to add to the issue.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(integer("id").description("The ID of the comment.")
            .required(false),
            string("body").description("The body of the comment.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private GitlabCreateCommentOnIssueAction() {
    }
}

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

package com.bytechef.component.gitlab.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GitlabCreateCommentOnIssueAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createCommentOnIssue")
        .title("Create Comment on Issue")
        .description("Adds a comment to the specified issue.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/projects/{projectId}/issues/{issueId}/notes"

            ))
        .properties(string("projectId").label("Project")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            integer("issueId").label("Issue")
                .description("The issue to comment on.")
                .required(true)
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
        .output(outputSchema(object()
            .properties(object("body").properties(integer("id").required(false), string("body").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private GitlabCreateCommentOnIssueAction() {
    }
}

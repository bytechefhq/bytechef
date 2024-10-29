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

package com.bytechef.component.jira.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.CREATE_ISSUE_COMMENT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vihar Shah
 */
public class JiraCreateIssueCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ISSUE_COMMENT)
        .title("Create issue comment")
        .description("Adds a comment to an issue.")
        .properties(
            string("issueIdOrKey")
                .label("Issue ID or Issue Key")
                .description(
                    "Issue ID or key of issue to add comment to, e.g. 10105 (issue ID) or ABC-123 (issue key). Use issue ID if your issue moves between projects.")
                .required(true),
            string("comment")
                .label("Comment")
                .description("The text of the comment.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("self"),
                        string("id"),
                        object("author")
                            .properties(
                                string("accountId"),
                                bool("active"),
                                string("displayName"),
                                string("self")),
                        string("body"),
                        object("updateAuthor")
                            .properties(
                                string("accountId"),
                                bool("active"),
                                string("displayName"),
                                string("self")),
                        string("created"),
                        string("updated"),
                        object("visibility")
                            .properties(
                                string("identifier"),
                                string("type"),
                                string("value")))))
        .perform(JiraCreateIssueCommentAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String issueId = inputParameters.getRequiredString("issueIdOrKey");
        String comment = inputParameters.getRequiredString("comment");

        Map<String, Object> inputs = new HashMap<>();
        inputs.put(
            "body", Map.of(
                "content", List.of(
                    Map.of(
                        "content", List.of(
                            "text", comment,
                            "type", "text"),
                        "type", "paragraph"))));

        return context
            .http(http -> http.post("/issue/" + issueId + "/comment"))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .body(Context.Http.Body.of(inputs, Context.Http.BodyContentType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

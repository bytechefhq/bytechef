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

package com.bytechef.component.jira.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.COMMENT;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Vihar Shah
 */
public class JiraCreateIssueCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createIssueComment")
        .title("Create Issue Comment")
        .description("Adds a comment to an issue.")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where the issue is located.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(false),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("ID of the issue where the comment will be added.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(COMMENT)
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

    private JiraCreateIssueCommentAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/issue/" + inputParameters.getRequiredString(ISSUE_ID) + "/comment"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(
                "body", Map.of(
                    "version", 1,
                    TYPE, "doc",
                    "content", List.of(
                        Map.of(
                            "content", List.of(Map.of(TEXT, inputParameters.getRequiredString(COMMENT), TYPE, TEXT)),
                            TYPE, "paragraph")))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

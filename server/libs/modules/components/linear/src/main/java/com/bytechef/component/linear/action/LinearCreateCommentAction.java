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

package com.bytechef.component.linear.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linear.constant.LinearConstants.BODY;
import static com.bytechef.component.linear.constant.LinearConstants.DATA;
import static com.bytechef.component.linear.constant.LinearConstants.ISSUE_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;
import static com.bytechef.component.linear.util.LinearUtils.executeGraphQLQuery;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linear.util.LinearUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class LinearCreateCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createComment")
        .title("Create Comment")
        .description("Creates a new comment.")
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("The ID of the team where this issue should be created.")
                .options((OptionsFunction<String>) LinearUtils::getTeamOptions)
                .required(true),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("The identifier of the issue to update.")
                .options((OptionsFunction<String>) LinearUtils::getIssueOptions)
                .optionsLookupDependsOn(TEAM_ID)
                .required(true),
            string(BODY)
                .label("Comment Body")
                .description("The comment content in markdown format.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description("Whether the operation was successful."),
                        object("comment")
                            .description("The comment that was created.")
                            .properties(
                                string("id")
                                    .description("The ID of the comment."),
                                object("issue")
                                    .description("The issue ID that the comment is associated with.")
                                    .properties(string("id")),
                                string("body")
                                    .description("The comment content in markdown format.")))))
        .perform(LinearCreateCommentAction::perform);

    private LinearCreateCommentAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query =
            "mutation{commentCreate(input: {issueId: \"%s\", body: \"%s\"}){success comment{id issue{id} body}}}"
                .formatted(inputParameters.getRequiredString(ISSUE_ID), inputParameters.getRequiredString(BODY));

        Map<String, Object> body = executeGraphQLQuery(query, context);

        if (body.get(DATA) instanceof Map<?, ?> data && data.get("commentCreate") instanceof Map<?, ?> commentCreate) {
            return commentCreate;
        }

        return null;
    }
}

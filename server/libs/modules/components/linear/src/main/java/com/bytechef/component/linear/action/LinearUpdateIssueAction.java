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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linear.constant.LinearConstants.ASSIGNEE_ID;
import static com.bytechef.component.linear.constant.LinearConstants.DATA;
import static com.bytechef.component.linear.constant.LinearConstants.DESCRIPTION;
import static com.bytechef.component.linear.constant.LinearConstants.ISSUE_ID;
import static com.bytechef.component.linear.constant.LinearConstants.PRIORITY;
import static com.bytechef.component.linear.constant.LinearConstants.STATUS_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TITLE;
import static com.bytechef.component.linear.util.LinearUtils.appendOptionalField;
import static com.bytechef.component.linear.util.LinearUtils.executeGraphQLQuery;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linear.util.LinearUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class LinearUpdateIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateIssue")
        .title("Update Issue")
        .description("Update an issue.")
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("The ID of the team where this issue should be created.")
                .options((ActionOptionsFunction<String>) LinearUtils::getTeamOptions)
                .required(true),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("The identifier of the issue to update.")
                .options((ActionOptionsFunction<String>) LinearUtils::getIssueOptions)
                .optionsLookupDependsOn(TEAM_ID)
                .required(true),
            string(TITLE)
                .label("Issue Title")
                .description("The title of the new issue.")
                .required(false),
            string(STATUS_ID)
                .label("Status")
                .description("The status of the issue.")
                .options((ActionOptionsFunction<String>) LinearUtils::getIssueStateOptions)
                .required(false),
            integer(PRIORITY)
                .label("Priority")
                .description("The priority of the issue.")
                .options(
                    option("No priority", 0),
                    option("Urgent", 1),
                    option("High", 2),
                    option("Normal", 3),
                    option("Low", 4))
                .required(false),
            string(ASSIGNEE_ID)
                .label("Assignee ID")
                .description("The identifier of the user to assign the issue to.")
                .options((ActionOptionsFunction<String>) LinearUtils::getAssigneeOptions)
                .required(false),
            string(DESCRIPTION)
                .label("Description")
                .description("The detailed description of the issue.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description(
                                "Whether the operation was successful."),
                        object("issue")
                            .description("The issue that was created or updated.")
                            .properties(
                                string("id")
                                    .description("The ID of the issue."),
                                string("title")
                                    .description("The title of the issue.")))))
        .perform(LinearUpdateIssueAction::perform);

    private LinearUpdateIssueAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String query = buildGraphQLQuery(inputParameters);
        Map<String, Object> body = executeGraphQLQuery(query, context);

        if (body.get(DATA) instanceof Map<?, ?> data && data.get("issueUpdate") instanceof Map<?, ?> issueUpdate) {
            return issueUpdate;
        }

        return null;
    }

    private static String buildGraphQLQuery(Parameters inputParameters) {
        StringBuilder sb = new StringBuilder("mutation{issueUpdate(input: { ");

        appendOptionalField(sb, TITLE, inputParameters.getString(TITLE));
        appendOptionalField(sb, "stateId", inputParameters.getString(STATUS_ID));
        appendOptionalField(sb, PRIORITY, inputParameters.getInteger(PRIORITY));
        appendOptionalField(sb, ASSIGNEE_ID, inputParameters.getString(ASSIGNEE_ID));
        appendOptionalField(sb, DESCRIPTION, inputParameters.getString(DESCRIPTION));

        String query = sb.toString();

        if (query.endsWith(", ")) {
            sb.setLength(sb.length() - 2);
        }

        sb.append(" } id: \"")
            .append(inputParameters.getRequiredString(ISSUE_ID))
            .append("\"){success issue{id title}}}");

        return sb.toString();
    }
}

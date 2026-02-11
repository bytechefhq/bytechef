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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.MAX_RESULTS;
import static com.bytechef.component.jira.constant.JiraConstants.ORDER_BY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.jira.util.JiraOptionsUtils;

/**
 * @author Ivona Pavela
 */
public class JiraListIssueCommentsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listIssueComments")
        .title("List Issue Comments")
        .description("Return all comments for an issue.")
        .help("", "https://docs.bytechef.io/reference/components/jira_v1#list-issue-comments")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where the issue is located.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(false),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("ID of the issue.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(ORDER_BY)
                .label("Order By")
                .description("Order the results by a field.")
                .options(
                    option("Created (Ascending)", "+created", "Order ascending by created date."),
                    option("Created (Descending)", "-created", "Order descending by created date."))
                .required(false),
            integer(MAX_RESULTS)
                .label("Max Results")
                .description("The maximum number of items to return per page.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("maxResults")
                            .description("The maximum number of items that could be returned."),
                        integer("startAt")
                            .description("The index of the first item returned."),
                        integer("total")
                            .description("The number of items returned."),
                        array("comments")
                            .description("List of comments on the issue")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("The ID of the comment."),
                                        string("self")
                                            .description("The URL of the comment."),
                                        object("body")
                                            .properties(
                                                string("type")
                                                    .description(
                                                        "Defines the type of block node such as paragraph, table, and alike."),
                                                integer("version")
                                                    .description(
                                                        "Defines the version of ADF used in this representation."),
                                                array("content")
                                                    .description(
                                                        "An array containing inline and block nodes that define the content of a section of the document.")
                                                    .items(
                                                        object()
                                                            .properties(string("text")))),
                                        object("author")
                                            .properties(
                                                string("accountId")
                                                    .description(
                                                        "The account ID of the user, which uniquely identifies the user across all Atlassian products."),
                                                string("accountType")
                                                    .description(
                                                        "The type of account represented by this user. This will be one of 'atlassian' (normal users), 'app' (application user) or 'customer' (Jira Service Desk customer user)"),
                                                bool("active")
                                                    .description("Whether the user is active."),
                                                string("emailAddress")
                                                    .description(
                                                        "The email address of the user. Depending on the user’s privacy settings, this may be returned as null."),
                                                string("displayName")
                                                    .description(
                                                        "The display name of the user. Depending on the user’s privacy settings, this may return an alternative value."),
                                                string("self")
                                                    .description("The URL of the user."),
                                                string("timezone")
                                                    .description(
                                                        "The time zone specified in the user's profile. Depending on the user’s privacy settings, this may be returned as null.")),
                                        string("created")
                                            .description("The date and time at which the comment was created."),
                                        string("updated")
                                            .description(
                                                "The date and time at which the comment was updated last."))))))
        .perform(JiraListIssueCommentsAction::perform);

    private JiraListIssueCommentsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/issue/" + inputParameters.getRequiredString(ISSUE_ID) + "/comment"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .queryParameters(
                ORDER_BY, inputParameters.getString(ORDER_BY),
                MAX_RESULTS, inputParameters.getInteger(MAX_RESULTS))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

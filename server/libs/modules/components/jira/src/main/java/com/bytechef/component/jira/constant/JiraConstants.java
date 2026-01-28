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

package com.bytechef.component.jira.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class JiraConstants {

    public static final String ASSIGNEE = "assignee";
    public static final String ADD = "add";
    public static final String ADD_LABELS = "addLabels";
    public static final String ACCOUNT_ID = "accountId";
    public static final String COMMENT = "comment";
    public static final String CONTENT = "content";
    public static final String DESCRIPTION = "description";
    public static final String FIELDS = "fields";
    public static final String ID = "id";
    public static final String ISSUE = "issue";
    public static final String ISSUES = "issues";
    public static final String ISSUE_ID = "issueId";
    public static final String ISSUETYPE = "issuetype";
    public static final String JQL = "jql";
    public static final String KEY = "key";
    public static final String MAX_RESULTS = "maxResults";
    public static final String NAME = "name";
    public static final String NEXT_PAGE = "nextPage";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PARENT = "parent";
    public static final String PRIORITY = "priority";
    public static final String PROJECT = "project";
    public static final String REMOVE = "remove";
    public static final String REMOVE_LABELS = "removeLabels";
    public static final String SELF = "self";
    public static final String SUMMARY = "summary";
    public static final String TEXT = "text";
    public static final String TYPE = "type";
    public static final String ORDER_BY = "orderBy";
    public static final String UPDATE = "update";

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the issue."),
            string(KEY)
                .description("The key of the issue."),
            string(SELF)
                .description("The URL of the issue details."),
            object(FIELDS)
                .properties(
                    object(ISSUETYPE)
                        .properties(
                            string(ID)
                                .description("ID of the issue type."),
                            string(NAME)
                                .description("Name of the issue type.")),
                    object(PROJECT)
                        .properties(
                            string(ID)
                                .description("ID of the project."),
                            string(NAME)
                                .description("Name of the project.")),
                    object(PRIORITY)
                        .properties(
                            string(ID)
                                .description("ID of the priority."),
                            string(NAME)
                                .description("Name of the priority.")),
                    object(ASSIGNEE)
                        .properties(
                            string("accountId")
                                .description("Account ID of the assignee."),
                            string("displayName")
                                .description("Display name of the assignee.")),
                    string(SUMMARY)
                        .description("Summary of the issue.")));

    private JiraConstants() {
    }
}

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

package com.bytechef.component.jira.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class JiraConstants {

    public static final String ASSIGNEE = "assignee";
    public static final String CONTENT = "content";
    public static final String CREATE_ISSUE = "createIssue";
    public static final String CREATE_ISSUE_COMMENT = "createIssueComment";
    public static final String DESCRIPTION = "description";
    public static final String FIELDS = "fields";
    public static final String GET_ISSUE = "getIssue";
    public static final String ID = "id";
    public static final String ISSUE = "issue";
    public static final String ISSUES = "issues";
    public static final String ISSUE_ID = "issueId";
    public static final String ISSUETYPE = "issuetype";
    public static final String JIRA = "jira";
    public static final String JQL = "jql";
    public static final String KEY = "key";
    public static final String MAX_RESULTS = "maxResults";
    public static final String NAME = "name";
    public static final String NEW_ISSUE = "newIssue";
    public static final String PARENT = "parent";
    public static final String PRIORITY = "priority";
    public static final String PROJECT = "project";
    public static final String SEARCH_FOR_ISSUES_USING_JQL = "searchForIssuesUsingJql";
    public static final String SUMMARY = "summary";
    public static final String TEXT = "text";
    public static final String TYPE = "type";
    public static final String UPDATED_ISSUE = "updatedIssue";
    public static final String YOUR_DOMAIN = "yourDomain";

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID),
            string(KEY),
            object(FIELDS)
                .properties(
                    object(ISSUETYPE)
                        .properties(
                            string(ID),
                            string(NAME)),
                    object(PROJECT)
                        .properties(
                            string(ID),
                            string(NAME)),
                    object(PRIORITY)
                        .properties(
                            string(ID),
                            string(NAME)),
                    object(ASSIGNEE)
                        .properties(
                            string(ID),
                            string(NAME)),
                    object(DESCRIPTION)
                        .properties(
                            string(TYPE),
                            array(CONTENT)
                                .items(
                                    object()
                                        .properties(
                                            array(CONTENT)
                                                .items(
                                                    object()
                                                        .properties(
                                                            string(TEXT),
                                                            string(TYPE))),
                                            string(TYPE))))));

    private JiraConstants() {
    }
}

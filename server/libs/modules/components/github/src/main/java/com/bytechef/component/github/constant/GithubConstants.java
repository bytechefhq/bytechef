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

package com.bytechef.component.github.constant;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Luka Ljubić
 */
public class GithubConstants {

    public static final String BODY = "body";
    public static final String CREATE_ISSUE = "createIssue";
    public static final String CREATE_COMMENT_ON_ISSUE = "createCommentOnIssue";
    public static final String ADD_ASSIGNEES_TO_ISSUE = "addAssigneesToIssue";
    public static final String GITHUB = "github";
    public static final String GET_ISSUE = "getIssue";
    public static final String ID = "id";
    public static final String ISSUE = "issue";
    public static final String ASSIGNEES = "assignees";
    public static final String NEW_ISSUE = "newIssue";
    public static final String NEW_PULL_REQUEST = "newPullRequest";
    public static final String REPOSITORY = "repository";
    public static final String TITLE = "title";

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string("url"),
            string("repository_url"),
            number(ID),
            integer("number"),
            string(TITLE),
            string("state"),
            string(BODY));

    private GithubConstants() {
    }
}

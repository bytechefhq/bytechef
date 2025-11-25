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

package com.bytechef.component.github.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Luka Ljubić
 */
public class GithubConstants {

    public static final String ASSIGNEES = "assignees";
    public static final String BODY = "body";
    public static final String EVENTS = "events";
    public static final String FILTER = "filter";
    public static final String ID = "id";
    public static final String ISSUE = "issue";
    public static final String LABELS = "labels";
    public static final String NAME = "name";
    public static final String OWNER = "owner";
    public static final String PULL_REQUESTS = "pullRequests";
    public static final String REPOSITORY = "repository";
    public static final String STATE = "state";
    public static final String TITLE = "title";
    public static final String HEAD = "head";
    public static final String HEAD_REPO = "head_repo";
    public static final String BASE = "base";
    public static final String DRAFT = "draft";

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string("url")
                .description("The URL linking directly to the issue on GitHub."),
            string("repository_url")
                .description("The URL of the repository where the issue is located."),
            number(ID)
                .description("ID of the issue."),
            integer("number")
                .description("A unique number identifying the issue within its repository."),
            string(TITLE)
                .description("The title or headline of the issue."),
            string("state")
                .description("The current state of the issue, such as open or closed."),
            array(ASSIGNEES)
                .description("A list of users assigned to the issue.")
                .items(
                    object()
                        .properties(
                            string("login")
                                .description("The username of the assignee."),
                            string(ID)
                                .description("ID of the assignee."),
                            string("html_url")
                                .description("The URL to the assignee's profile page."),
                            string("type")
                                .description("The type of user, e.g., User or Organization."))),
            array(LABELS)
                .description("A collection of labels associated with the issue.")
                .items(
                    object()
                        .properties(
                            string(ID)
                                .description("ID of the label."),
                            string(NAME)
                                .description("Name of the label"),
                            string("description")
                                .description("A brief description of the label's purpose."))),
            string(BODY)
                .description("The main content of the issue."));

    public static final ModifiableStringProperty OWNER_PROPERTY = string(OWNER)
        .label("User/Organization")
        .description("The owner of the repository (user or organization).")
        .exampleValue("bytechefhq")
        .required(true);

    private GithubConstants() {
    }
}

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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Luka Ljubić
 */
public class GithubConstants {

    public static final String ASSIGNEES = "assignees";
    public static final String BODY = "body";
    public static final String FILTER = "filter";
    public static final String ID = "id";
    public static final String ISSUE = "issue";
    public static final String LABELS = "labels";
    public static final String NAME = "name";
    public static final String OWNER = "owner";
    public static final String REPOSITORY = "repository";
    public static final String STATE = "state";
    public static final String TITLE = "title";

    public static final ModifiableStringProperty FILTER_PROPERTY = string(FILTER)
        .label("Filter")
        .description("Specifies the types of issues to return.")
        .options(
            option("Assigned", "assigned",
                "Issues assigned to the authenticated user."),
            option("Created", "created",
                "Issues created by the authenticated user."),
            option("Mentioned", "mentioned",
                "Issues mentioning the authenticated user."),
            option("Subscribed", "subscribed",
                "Issues the authenticated user is subscribed to updates for."),
            option("Repos", "repos",
                "All issues from repositories that the authenticated user has explicit access to."),
            option("All", "all",
                "All issues related to the authenticated user."))
        .required(true);

    public static final ModifiableStringProperty STATE_PROPERTY = string(STATE)
        .label("State")
        .description("Indicates the state of the issues to return.")
        .options(
            option("Open", "open",
                "Open issues."),
            option("Closed", "closed",
                "Closed issues."),
            option("All", "all",
                "All issues."))
        .required(true);

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string("url"),
            string("repository_url"),
            number(ID),
            integer("number"),
            string(TITLE),
            string("state"),
            array(ASSIGNEES)
                .items(
                    object()
                        .properties(
                            string("login"),
                            string(ID),
                            string("html_url"),
                            string("type"))),
            array(LABELS)
                .items(
                    object()
                        .properties(
                            string(ID),
                            string(NAME),
                            string("description"))),
            string(BODY));

    private GithubConstants() {
    }
}

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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.github.constant.GithubConstants.BASE;
import static com.bytechef.component.github.constant.GithubConstants.DIRECTION;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PULL_REQUEST_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.SORT;
import static com.bytechef.component.github.constant.GithubConstants.STATE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.github.util.GithubUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubListPullRequestsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listPullRequests")
        .title("List Pull Requests")
        .description("Lists pull requests in a repository, optionally filtered by state and base branch.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#list-pull-requests")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository whose pull requests are listed.")
                .required(true),
            string(STATE)
                .label("State")
                .description("Filters pull requests by state.")
                .options(
                    option("Open", "open"),
                    option("Closed", "closed"),
                    option("All", "all"))
                .defaultValue("open")
                .required(false),
            string(BASE)
                .label("Base")
                .description("Filters pull requests by the name of the branch they are merged into, for example main.")
                .required(false),
            string(SORT)
                .label("Sort")
                .description(
                    "What to sort results by. Popularity sorts by the number of comments. Long-running sorts by " +
                        "creation date and only includes pull requests open for more than a month with activity " +
                        "in the past month.")
                .options(
                    option("Created", "created"),
                    option("Updated", "updated"),
                    option("Popularity", "popularity"),
                    option("Long-running", "long-running"))
                .required(false),
            string(DIRECTION)
                .label("Direction")
                .description(
                    "The direction of the sort. Defaults to descending when sorting by created date or when no " +
                        "sort is given, otherwise ascending.")
                .options(
                    option("Ascending", "asc"),
                    option("Descending", "desc"))
                .required(false))
        .output(
            outputSchema(
                array()
                    .description("List of pull requests in the repository.")
                    .items(PULL_REQUEST_OUTPUT_PROPERTY)))
        .perform(GithubListPullRequestsAction::perform);

    private GithubListPullRequestsAction() {
    }

    public static List<Map<String, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String url = "/repos/%s/%s/pulls".formatted(
            inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY));

        return GithubUtils.getItems(
            context, url, false,
            STATE, inputParameters.getString(STATE, "open"),
            BASE, inputParameters.getString(BASE),
            SORT, inputParameters.getString(SORT),
            DIRECTION, inputParameters.getString(DIRECTION));
    }
}

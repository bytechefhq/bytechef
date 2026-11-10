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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.LABELS;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.ORDER;
import static com.bytechef.component.github.constant.GithubConstants.PAGE;
import static com.bytechef.component.github.constant.GithubConstants.PER_PAGE;
import static com.bytechef.component.github.constant.GithubConstants.QUERY;
import static com.bytechef.component.github.constant.GithubConstants.SORT;
import static com.bytechef.component.github.constant.GithubConstants.STATE;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubSearchIssuesAndPullRequestsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchIssuesAndPullRequests")
        .title("Search Issues and Pull Requests")
        .description(
            "Searches issues and pull requests across GitHub using GitHub search syntax and returns up to 100 " +
                "results per page.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#search-issues-and-pull-requests")
        .properties(
            string(QUERY)
                .label("Query")
                .description(
                    "The search keywords and qualifiers, for example 'repo:bytechefhq/bytechef is:pr is:open " +
                        "review:required'. Use is:issue or is:pr to limit results to issues or pull requests.")
                .required(true),
            string(SORT)
                .label("Sort")
                .description("What to sort results by. Results are sorted by best match when not set.")
                .options(
                    option("Comments", "comments"),
                    option("Reactions", "reactions"),
                    option("Interactions", "interactions"),
                    option("Created", "created"),
                    option("Updated", "updated"))
                .required(false),
            string(ORDER)
                .label("Order")
                .description("The order of the results. Only used when Sort is set.")
                .options(
                    option("Descending", "desc"),
                    option("Ascending", "asc"))
                .required(false),
            integer(PAGE)
                .label("Page")
                .description("The page number of the results to fetch.")
                .required(false),
            integer(PER_PAGE)
                .label("Per Page")
                .description("The number of results per page (max 100).")
                .maxValue(100)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("total_count")
                            .description("Total number of matching results."),
                        bool("incomplete_results")
                            .description("Whether the results are incomplete."),
                        array("items")
                            .description("Issues and pull requests that match the query.")
                            .items(
                                object()
                                    .properties(
                                        number(ID)
                                            .description("ID of the issue or pull request."),
                                        integer("number")
                                            .description("The number of the issue or pull request in its repository."),
                                        string(TITLE)
                                            .description("The title of the issue or pull request."),
                                        string(STATE)
                                            .description("The state, either open or closed."),
                                        string(BODY)
                                            .description("The contents of the issue or pull request."),
                                        string("html_url")
                                            .description("The URL of the issue or pull request on GitHub."),
                                        string("repository_url")
                                            .description("The API URL of the repository."),
                                        object("user")
                                            .description("The user who opened the issue or pull request.")
                                            .properties(
                                                string("login"),
                                                number(ID),
                                                string("html_url")),
                                        array(LABELS)
                                            .items(
                                                object()
                                                    .properties(
                                                        number(ID),
                                                        string(NAME))),
                                        object("pull_request")
                                            .description("Present only when the result is a pull request.")
                                            .properties(
                                                string("url"),
                                                string("html_url"),
                                                string("merged_at")),
                                        integer("comments")
                                            .description("The number of comments."),
                                        string("created_at")
                                            .description("When it was created."),
                                        string("updated_at")
                                            .description("When it was last updated."),
                                        string("closed_at")
                                            .description("When it was closed."),
                                        number("score")
                                            .description("Search relevance score."))))))
        .perform(GithubSearchIssuesAndPullRequestsAction::perform);

    private GithubSearchIssuesAndPullRequestsAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.get("/search/issues"))
            .queryParameters(
                "q", inputParameters.getRequiredString(QUERY),
                SORT, inputParameters.getString(SORT),
                ORDER, inputParameters.getString(ORDER),
                PAGE, inputParameters.getInteger(PAGE),
                PER_PAGE, inputParameters.getInteger(PER_PAGE))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

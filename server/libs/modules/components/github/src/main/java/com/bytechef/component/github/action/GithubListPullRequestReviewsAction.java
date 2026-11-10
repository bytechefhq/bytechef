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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PULL_NUMBER;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
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
public class GithubListPullRequestReviewsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listPullRequestReviews")
        .title("List Pull Request Reviews")
        .description("Lists the reviews of a pull request in chronological order.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#list-pull-request-reviews")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository the pull request belongs to.")
                .required(true),
            integer(PULL_NUMBER)
                .label("Pull Request Number")
                .description("The number that identifies the pull request within the repository.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .description("List of reviews of the pull request.")
                    .items(
                        object()
                            .properties(
                                number(ID)
                                    .description("ID of the review."),
                                object("user")
                                    .description("The user who submitted the review.")
                                    .properties(
                                        string("login"),
                                        number(ID),
                                        string("html_url")),
                                string(BODY)
                                    .description("The body text of the review."),
                                string(STATE)
                                    .description(
                                        "The state of the review, such as APPROVED, CHANGES_REQUESTED, " +
                                            "COMMENTED, DISMISSED or PENDING."),
                                string("submitted_at")
                                    .description("When the review was submitted."),
                                string("commit_id")
                                    .description("The SHA of the commit the review was made on."),
                                string("author_association")
                                    .description("How the reviewer is associated with the repository."),
                                string("html_url")
                                    .description("The URL of the review on GitHub.")))))
        .perform(GithubListPullRequestReviewsAction::perform);

    private GithubListPullRequestReviewsAction() {
    }

    public static List<Map<String, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String url = "/repos/%s/%s/pulls/%s/reviews".formatted(
            inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY),
            inputParameters.getRequiredInteger(PULL_NUMBER));

        return GithubUtils.getItems(context, url, false);
    }
}

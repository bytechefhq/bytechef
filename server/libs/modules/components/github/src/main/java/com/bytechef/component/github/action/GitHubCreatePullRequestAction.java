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

import com.bytechef.component.definition.*;
import com.bytechef.component.github.util.GithubUtils;

import java.util.Map;

import static com.bytechef.component.definition.ComponentDsl.*;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.*;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

/**
 * @author Anas Elgarhy (@0x61nas)
 */
public class GitHubCreatePullRequestAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createPullRequest")
        .title("Create Pull Request")
        .description("Create Pull Request in GitHub Repository")
        .properties(
            string(REPOSITORY)
                .label("Repository")
                .description("Repository where new pull request will be created.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Title of the pull request.")
                .maxLength(100)
                .required(false),
            string(BODY)
                .label("Description")
                .description("The description of the pull request.")
                .required(false),
            string(HEAD)
                .label("Head")
                .description("The name of the branch where your changes are implemented.")
                .required(true),
            string(HEAD_REPO)
                .label("Head Repo")
                .description("The name of the repository where the changes in the pull request were made.")
                .required(false),
            string(BASE)
                .label("Base")
                .description("The name of the branch you want the changes pulled into.")
                .required(true),
            integer(ISSUE)
                .label("Issue")
                .description("An issue in the repository to convert to a pull request.")
                .required(false),
            bool(DRAFT)
                .label("Draft")
                .description("Indicates whether the pull request is a draft.")
                .defaultValue(false)
                .required(true)
        )
        .output(outputSchema(PULL_REQUEST_OUTPUT_PROBERTY))
        .perform(GitHubCreatePullRequestAction::perform);

    private GitHubCreatePullRequestAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/pulls"))
            .body(
                Context.Http.Body.of(
                    TITLE, inputParameters.getString(TITLE),
                    HEAD, inputParameters.getRequiredString(HEAD),
                    HEAD_REPO, inputParameters.getString(HEAD_REPO),
                    BASE, inputParameters.getRequiredString(BASE),
                    BODY, inputParameters.getString(BODY),
                    DRAFT, inputParameters.getBoolean(DRAFT),
                    ISSUE, inputParameters.getInteger(ISSUE)))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {
            });
    }
}

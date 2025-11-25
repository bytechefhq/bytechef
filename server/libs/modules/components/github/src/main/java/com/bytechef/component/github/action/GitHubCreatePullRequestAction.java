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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BASE;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.DRAFT;
import static com.bytechef.component.github.constant.GithubConstants.HEAD;
import static com.bytechef.component.github.constant.GithubConstants.HEAD_REPO;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Anas Elgarhy (@0x61nas)
 * @author Monika Kušter
 */
public class GitHubCreatePullRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPullRequest")
        .title("Create Pull Request")
        .description("Creates a new pull request.")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository where new pull request will be created.")
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Title of the new pull request.")
                .maxLength(100)
                .required(false),
            string(BODY)
                .label("Body")
                .description("The contents of the pull request.")
                .controlType(ControlType.TEXT_AREA)
                .required(false),
            string(HEAD)
                .label("Head")
                .description(
                    "The name of the branch where your changes are implemented. For cross-repository pull requests " +
                        "in the same network, namespace head with a user like this: username:branch.")
                .required(true),
            string(HEAD_REPO)
                .label("Head Repo")
                .description(
                    "The name of the repository where the changes in the pull request were made. This field is " +
                        "required for cross-repository pull requests if both repositories are owned by the same " +
                        "organization.")
                .required(false),
            string(BASE)
                .label("Base")
                .description(
                    "The name of the branch you want the changes pulled into. This should be an existing branch " +
                        "on the current repository. You cannot submit a pull request to one repository that " +
                        "requests a merge to a base of another repository.")
                .required(true),
            integer(ISSUE)
                .label("Issue")
                .description(
                    "An issue in the repository to convert to a pull request. The issue title, body, " +
                        "and comments will become the title, body, and comments on the new pull request. " +
                        "Required unless title is specified.")
                .required(false),
            bool(DRAFT)
                .label("Draft")
                .description("Indicates whether the pull request is a draft.")
                .defaultValue(false)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("url")
                            .description("The URL of the created pull request."),
                        integer(ID)
                            .description("ID of the created pull request."),
                        string("node_id"),
                        string("html_url"),
                        string("diff_url"),
                        string("patch_url"),
                        string("issue_url"),
                        string("commits_url"),
                        string("review_comment_url"),
                        string("comments_url"),
                        string("statuses_url"),
                        integer("number")
                            .description("Number uniquely identifying the pull request within its repository."),
                        string("state")
                            .description("The state of the pull request. Either open or closed."),
                        bool("locked")
                            .description(""),
                        string("title")
                            .description("The title of the pull request."),
                        object("user")
                            .description("A GitHub user.")
                            .properties(
                                string("login")
                                    .description("The username of the assignee."),
                                string(ID)
                                    .description("ID of the assignee."),
                                string("html_url")
                                    .description("The URL to the assignee's profile page."),
                                string("type")
                                    .description("The type of user, e.g., User or Organization.")),
                        string("body")
                            .description("The contents of the pull request."),
                        array("labels")
                            .items(
                                object()
                                    .properties(
                                        string(ID)
                                            .description("ID of the label."),
                                        string(NAME)
                                            .description("Name of the label"),
                                        string("description")
                                            .description("A brief description of the label's purpose."))),
                        integer("comments")
                            .description("The number of comments on the pull request."),
                        integer("review_comments")
                            .description("The number of comments for review on the pull request."),
                        bool("maintainer_can_modify")
                            .description("Indicates whether maintainers can modify the pull request."),
                        integer("commits")
                            .description("The number of commits in the pull request."),
                        integer("additions")
                            .description("The number of additions in the pull request."),
                        integer("deletions")
                            .description("The number of deletions in the pull request."),
                        integer("changed_files")
                            .description("The number of changed files in the pull request."))))
        .perform(GitHubCreatePullRequestAction::perform);

    private GitHubCreatePullRequestAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "/repos/%s/%s/pulls".formatted(
                    inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY))))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getString(TITLE),
                    HEAD, inputParameters.getRequiredString(HEAD),
                    HEAD_REPO, inputParameters.getString(HEAD_REPO),
                    BASE, inputParameters.getRequiredString(BASE),
                    BODY, inputParameters.getString(BODY),
                    DRAFT, inputParameters.getBoolean(DRAFT),
                    ISSUE, inputParameters.getInteger(ISSUE)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

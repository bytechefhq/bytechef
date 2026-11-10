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
import static com.bytechef.component.definition.ComponentDsl.bool;
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
    public static final String BASE = "base";
    public static final String BASE_BRANCH = "baseBranch";
    public static final String BODY = "body";
    public static final String COMMIT_MESSAGE = "commitMessage";
    public static final String CONTENT = "content";
    public static final String DEFAULT_BRANCH_ONLY = "defaultBranchOnly";
    public static final String DELETE = "delete";
    public static final String DIRECTION = "direction";
    public static final String DRAFT = "draft";
    public static final String EVENTS = "events";
    public static final String EXTENSION = "extension";
    public static final String FILE = "file";
    public static final String FILENAME = "filename";
    public static final String FILES = "files";
    public static final String FILTER = "filter";
    public static final String GENERATE_RELEASE_NOTES = "generateReleaseNotes";
    public static final String HEAD = "head";
    public static final String HEAD_REPO = "head_repo";
    public static final String ID = "id";
    public static final String IN = "in";
    public static final String INPUTS = "inputs";
    public static final String ISSUE = "issue";
    public static final String LABELS = "labels";
    public static final String MILESTONE = "milestone";
    public static final String NAME = "name";
    public static final String NEW_BRANCH = "newBranch";
    public static final String OUTPUT_FORMAT = "outputFormat";
    public static final String ORDER = "order";
    public static final String ORGANIZATION = "organization";
    public static final String OWNER = "owner";
    public static final String PAGE = "page";
    public static final String PATH = "path";
    public static final String PER_PAGE = "per_page";
    public static final String PRERELEASE = "prerelease";
    public static final String PULL_NUMBER = "pullNumber";
    public static final String PULL_REQUESTS = "pullRequests";
    public static final String QUERY = "query";
    public static final String REF = "ref";
    public static final String REPOSITORY = "repository";
    public static final String SORT = "sort";
    public static final String STATE = "state";
    public static final String TAG = "tag";
    public static final String TAG_NAME = "tagName";
    public static final String TITLE = "title";
    public static final String UPDATE_EXISTING_BRANCH = "updateExistingBranch";
    public static final String WORKFLOW = "workflow";

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

    public static final ModifiableObjectProperty PULL_REQUEST_OUTPUT_PROPERTY = object()
        .properties(
            number(ID)
                .description("ID of the pull request."),
            integer("number")
                .description("Number uniquely identifying the pull request within its repository."),
            string("url")
                .description("The API URL of the pull request."),
            string("html_url")
                .description("The URL of the pull request on GitHub."),
            string("diff_url")
                .description("The URL of the pull request diff."),
            string(STATE)
                .description("The state of the pull request. Either open or closed."),
            bool("locked")
                .description("Whether the pull request conversation is locked."),
            string(TITLE)
                .description("The title of the pull request."),
            string(BODY)
                .description("The contents of the pull request."),
            object("user")
                .description("The user who opened the pull request.")
                .properties(
                    string("login"),
                    number(ID),
                    string("html_url"),
                    string("type")),
            array(LABELS)
                .description("Labels associated with the pull request.")
                .items(
                    object()
                        .properties(
                            number(ID),
                            string(NAME),
                            string("description"))),
            array(ASSIGNEES)
                .description("Users assigned to the pull request.")
                .items(
                    object()
                        .properties(
                            string("login"),
                            number(ID),
                            string("html_url"))),
            array("requested_reviewers")
                .description("Users whose review is requested on the pull request.")
                .items(
                    object()
                        .properties(
                            string("login"),
                            number(ID),
                            string("html_url"))),
            array("requested_teams")
                .description("Teams whose review is requested on the pull request.")
                .items(
                    object()
                        .properties(
                            number(ID),
                            string(NAME),
                            string("slug"))),
            object(HEAD)
                .description("The branch where the changes are implemented.")
                .properties(
                    string("label"),
                    string(REF),
                    string("sha")),
            object(BASE)
                .description("The branch the changes are pulled into.")
                .properties(
                    string("label"),
                    string(REF),
                    string("sha")),
            bool(DRAFT)
                .description("Whether the pull request is a draft."),
            bool("merged")
                .description("Whether the pull request has been merged."),
            string("mergeable_state")
                .description("The mergeable state of the pull request."),
            string("merge_commit_sha")
                .description("The SHA of the merge commit."),
            string("created_at")
                .description("When the pull request was created."),
            string("updated_at")
                .description("When the pull request was last updated."),
            string("closed_at")
                .description("When the pull request was closed."),
            string("merged_at")
                .description("When the pull request was merged."),
            integer("comments")
                .description("The number of comments on the pull request."),
            integer("review_comments")
                .description("The number of review comments on the pull request."),
            integer("commits")
                .description("The number of commits in the pull request."),
            integer("additions")
                .description("The number of additions in the pull request."),
            integer("deletions")
                .description("The number of deletions in the pull request."),
            integer("changed_files")
                .description("The number of changed files in the pull request."));

    public static final ModifiableObjectProperty RELEASE_OUTPUT_PROPERTY = object()
        .properties(
            number(ID)
                .description("ID of the release."),
            string("tag_name")
                .description("The name of the tag."),
            string("target_commitish")
                .description("The branch or commit SHA the tag was created from."),
            string(NAME)
                .description("The name of the release."),
            string(BODY)
                .description("The release notes."),
            bool(DRAFT)
                .description("Whether the release is a draft."),
            bool("prerelease")
                .description("Whether the release is a prerelease."),
            string("created_at")
                .description("When the release was created."),
            string("published_at")
                .description("When the release was published."),
            string("html_url")
                .description("The URL of the release on GitHub."),
            string("tarball_url"),
            string("zipball_url"),
            object("author")
                .description("The user who created the release.")
                .properties(
                    string("login"),
                    number(ID),
                    string("html_url")),
            array("assets")
                .description("Files attached to the release.")
                .items(
                    object()
                        .properties(
                            number(ID),
                            string(NAME),
                            string("label"),
                            string("content_type"),
                            integer("size"),
                            integer("download_count"),
                            string("browser_download_url"))));

    public static final ModifiableStringProperty OWNER_PROPERTY = string(OWNER)
        .label("User/Organization")
        .description("The owner of the repository (user or organization).")
        .exampleValue("bytechefhq")
        .required(true);

    private GithubConstants() {
    }
}

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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BASE_BRANCH;
import static com.bytechef.component.github.constant.GithubConstants.COMMIT_MESSAGE;
import static com.bytechef.component.github.constant.GithubConstants.CONTENT;
import static com.bytechef.component.github.constant.GithubConstants.DELETE;
import static com.bytechef.component.github.constant.GithubConstants.FILES;
import static com.bytechef.component.github.constant.GithubConstants.NEW_BRANCH;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.UPDATE_EXISTING_BRANCH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.github.util.GithubUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubCommitFilesToNewBranchAction {

    private static final String FILE_MODE = "100644";
    private static final String REFS_HEADS = "refs/heads/";
    private static final String SHA = "sha";

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("commitFilesToNewBranch")
        .title("Commit Files to New Branch")
        .description(
            "Creates a branch from a base branch and commits a set of file changes to it in a single commit, ready " +
                "for Create Pull Request.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#commit-files-to-new-branch")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository to commit to.")
                .required(true),
            string(BASE_BRANCH)
                .label("Base Branch")
                .description("The branch to start from. Defaults to the repository's default branch.")
                .required(false),
            string(NEW_BRANCH)
                .label("New Branch")
                .description("The name of the branch to create, for example docs/release-1.0.")
                .required(true),
            string(COMMIT_MESSAGE)
                .label("Commit Message")
                .description("The commit message.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            array(FILES)
                .label("Files")
                .description("The files to add, change or delete.")
                .items(
                    object()
                        .properties(
                            string(PATH)
                                .label("Path")
                                .description("The file path in the repository, for example docs/release.md.")
                                .required(true),
                            string(CONTENT)
                                .label("Content")
                                .description("The full file content as UTF-8 text. Ignored when Delete is on.")
                                .controlType(ControlType.TEXT_AREA)
                                .required(false),
                            bool(DELETE)
                                .label("Delete")
                                .description("Delete the file at this path instead of writing it.")
                                .defaultValue(false)
                                .required(false)))
                .required(true),
            bool(UPDATE_EXISTING_BRANCH)
                .label("Update Existing Branch")
                .description(
                    "When the new branch already exists, add the commit on top of it instead of failing. The " +
                        "branch is only fast-forwarded, never force-updated.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("branch")
                            .description("The branch the commit was written to."),
                        string("commitSha")
                            .description("The SHA of the new commit."),
                        string("treeSha")
                            .description("The SHA of the new commit's tree."),
                        string("commitUrl")
                            .description("The URL of the new commit on GitHub."))))
        .perform(GithubCommitFilesToNewBranchAction::perform);

    private GithubCommitFilesToNewBranchAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String owner = inputParameters.getRequiredString(OWNER);
        String repository = inputParameters.getRequiredString(REPOSITORY);
        String newBranch = inputParameters.getRequiredString(NEW_BRANCH);
        String repositoryUrl = "/repos/%s/%s".formatted(owner, repository);

        String existingBranchSha = findBranchHeadSha(repositoryUrl, newBranch, context);

        if (existingBranchSha != null && !inputParameters.getBoolean(UPDATE_EXISTING_BRANCH, false)) {
            throw new ProviderException(
                "Branch '%s' already exists in %s/%s. Choose another branch name or turn on Update Existing Branch."
                    .formatted(newBranch, owner, repository));
        }

        String parentSha = existingBranchSha;

        if (parentSha == null) {
            parentSha = getBaseBranchHeadSha(inputParameters, owner, repository, repositoryUrl, context);
        }

        String parentTreeSha = getCommitTreeSha(repositoryUrl, parentSha, context);

        String treeSha = createTree(
            repositoryUrl, parentTreeSha, inputParameters.getRequiredList(FILES, new TypeReference<>() {}), context);

        Map<String, Object> commit = post(
            repositoryUrl + "/git/commits",
            Map.of(
                "message", inputParameters.getRequiredString(COMMIT_MESSAGE), "parents", List.of(parentSha), "tree",
                treeSha),
            context);

        String commitSha = (String) commit.get(SHA);

        if (existingBranchSha == null) {
            post(repositoryUrl + "/git/refs", Map.of("ref", REFS_HEADS + newBranch, SHA, commitSha), context);
        } else {
            context.http(http -> http.patch(repositoryUrl + "/git/refs/heads/" + newBranch))
                .body(Body.of(SHA, commitSha, "force", false))
                .configuration(responseType(ResponseType.JSON))
                .execute();
        }

        Map<String, Object> result = new HashMap<>();

        result.put("branch", newBranch);
        result.put("commitSha", commitSha);
        result.put("commitUrl", commit.get("html_url"));
        result.put("treeSha", treeSha);

        return result;
    }

    private static String createTree(
        String repositoryUrl, String baseTreeSha, List<Map<String, Object>> files, Context context) {

        List<Map<String, Object>> treeEntries = new ArrayList<>();

        for (Map<String, Object> file : files) {
            Map<String, Object> treeEntry = new HashMap<>();

            treeEntry.put("mode", FILE_MODE);
            treeEntry.put(PATH, file.get(PATH));
            treeEntry.put("type", "blob");

            if (Boolean.TRUE.equals(file.get(DELETE))) {
                treeEntry.put(SHA, null);
            } else {
                Object content = file.get(CONTENT);

                treeEntry.put(CONTENT, content == null ? "" : content.toString());
            }

            treeEntries.add(treeEntry);
        }

        Map<String, Object> tree = post(
            repositoryUrl + "/git/trees", Map.of("base_tree", baseTreeSha, "tree", treeEntries), context);

        return (String) tree.get(SHA);
    }

    private static String findBranchHeadSha(String repositoryUrl, String branch, Context context) {
        List<Map<String, Object>> refs = context
            .http(http -> http.get(repositoryUrl + "/git/matching-refs/heads/" + branch))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (refs == null) {
            return null;
        }

        for (Map<String, Object> ref : refs) {
            if ((REFS_HEADS + branch).equals(ref.get("ref")) && ref.get("object") instanceof Map<?, ?> object) {
                return (String) object.get(SHA);
            }
        }

        return null;
    }

    private static String getBaseBranchHeadSha(
        Parameters inputParameters, String owner, String repository, String repositoryUrl, Context context) {

        String baseBranch = inputParameters.getString(BASE_BRANCH);

        if (baseBranch == null || baseBranch.isBlank()) {
            baseBranch = GithubUtils.getDefaultBranch(owner, repository, context);
        }

        String baseRefUrl = repositoryUrl + "/git/ref/heads/" + baseBranch;

        Map<String, Object> ref = context.http(http -> http.get(baseRefUrl))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<?, ?> object = (Map<?, ?>) ref.get("object");

        return (String) object.get(SHA);
    }

    private static String getCommitTreeSha(String repositoryUrl, String commitSha, Context context) {
        Map<String, Object> commit = context.http(http -> http.get(repositoryUrl + "/git/commits/" + commitSha))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<?, ?> tree = (Map<?, ?>) commit.get("tree");

        return (String) tree.get(SHA);
    }

    private static Map<String, Object> post(String url, Map<String, ?> body, Context context) {
        return context.http(http -> http.post(url))
            .body(Body.of(body))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

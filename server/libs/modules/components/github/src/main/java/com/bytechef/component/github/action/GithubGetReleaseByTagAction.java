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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.DRAFT;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TAG;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubGetReleaseByTagAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getReleaseByTag")
        .title("Get Release by Tag")
        .description("Gets a published release by its tag name.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#get-release-by-tag")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository the release belongs to.")
                .required(true),
            string(TAG)
                .label("Tag")
                .description("The tag name of the release, for example v1.0.0.")
                .required(true))
        .output(
            outputSchema(
                object()
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
                                        string("browser_download_url"))))))
        .perform(GithubGetReleaseByTagAction::perform);

    private GithubGetReleaseByTagAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.get(
                "/repos/%s/%s/releases/tags/%s".formatted(
                    inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY),
                    inputParameters.getRequiredString(TAG))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.DRAFT;
import static com.bytechef.component.github.constant.GithubConstants.GENERATE_RELEASE_NOTES;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PRERELEASE;
import static com.bytechef.component.github.constant.GithubConstants.RELEASE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TAG_NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubCreateReleaseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRelease")
        .title("Create Release")
        .description(
            "Creates a release in a repository. The tag is created from the default branch if it does not exist.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#create-release")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository where the release will be created.")
                .required(true),
            string(TAG_NAME)
                .label("Tag Name")
                .description("The name of the tag for the release, for example v1.0.0.")
                .required(true),
            string(NAME)
                .label("Name")
                .description("The title of the release.")
                .required(false),
            string(BODY)
                .label("Body")
                .description(
                    "The release notes. When Generate Release Notes is enabled, this text is prepended to the " +
                        "generated notes.")
                .controlType(ControlType.TEXT_AREA)
                .required(false),
            bool(DRAFT)
                .label("Draft")
                .description("Whether to create an unpublished draft release.")
                .defaultValue(false)
                .required(false),
            bool(PRERELEASE)
                .label("Prerelease")
                .description("Whether to mark the release as a prerelease.")
                .defaultValue(false)
                .required(false),
            bool(GENERATE_RELEASE_NOTES)
                .label("Generate Release Notes")
                .description(
                    "Whether GitHub should generate the release name and notes from the merged pull requests " +
                        "since the previous release.")
                .defaultValue(false)
                .required(false))
        .output(outputSchema(RELEASE_OUTPUT_PROPERTY))
        .perform(GithubCreateReleaseAction::perform);

    private GithubCreateReleaseAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "/repos/%s/%s/releases".formatted(
                    inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY))))
            .body(
                Http.Body.of(
                    "tag_name", inputParameters.getRequiredString(TAG_NAME),
                    NAME, inputParameters.getString(NAME),
                    BODY, inputParameters.getString(BODY),
                    DRAFT, inputParameters.getBoolean(DRAFT),
                    PRERELEASE, inputParameters.getBoolean(PRERELEASE),
                    "generate_release_notes", inputParameters.getBoolean(GENERATE_RELEASE_NOTES)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.DEFAULT_BRANCH_ONLY;
import static com.bytechef.component.github.constant.GithubConstants.NAME;
import static com.bytechef.component.github.constant.GithubConstants.ORGANIZATION;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class GithubCreateForkAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createFork")
        .title("Create Fork")
        .description("Create a fork of Github repository.")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository that will be forked.")
                .required(true),
            string(NAME)
                .label("Name")
                .description("A new name for the fork.")
                .maxLength(100)
                .required(false),
            string(ORGANIZATION)
                .label("Organization")
                .description("The organization name if forking into an organization.")
                .required(false),
            bool(DEFAULT_BRANCH_ONLY)
                .label("Default branch only")
                .description("When forking from an existing repository, fork with only the default branch.")
                .defaultValue(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("The unique identifier of the fork."),
                        string("node_id")
                            .description("The node ID of the fork."),
                        string("name")
                            .description("The name of the forked repository."),
                        string("full_name")
                            .description("The full name of the forked repository including owner."),
                        object("owner")
                            .properties(
                                string("login").description("Username of the repository owner."),
                                string("id").description("ID of the repository owner."),
                                string("node_id").description("Node ID of the owner."),
                                string("url").description("URL of the owner."))
                            .description("Owner information."),
                        bool("private")
                            .description("Indicates if the forked repository is private."),
                        string("html_url")
                            .description("HTML URL of the forked repository."),
                        string("url")
                            .description("API URL of the forked repository."),
                        string("description")
                            .description("Description of the repository."),
                        string("fork")
                            .description("Whether this repository is a fork."),
                        string("created_at")
                            .description("Creation timestamp."),
                        string("updated_at")
                            .description("Last update timestamp."),
                        string("pushed_at")
                            .description("Last push timestamp."),
                        string("default_branch")
                            .description("Default branch name."))))
        .perform(GithubCreateForkAction::perform);

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "/repos/" + inputParameters.getRequiredString(OWNER) + "/"
                    + inputParameters.getRequiredString(REPOSITORY) + "/forks"))
            .body(Http.Body.of(
                NAME, inputParameters.getString(NAME),
                ORGANIZATION, inputParameters.getString(ORGANIZATION),
                DEFAULT_BRANCH_ONLY, inputParameters.getBoolean(DEFAULT_BRANCH_ONLY)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

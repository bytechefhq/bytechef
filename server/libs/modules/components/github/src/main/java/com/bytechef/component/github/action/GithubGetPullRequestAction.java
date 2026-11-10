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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.PULL_NUMBER;
import static com.bytechef.component.github.constant.GithubConstants.PULL_REQUEST_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GithubGetPullRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPullRequest")
        .title("Get Pull Request")
        .description("Gets a pull request by its number, including its requested reviewers.")
        .help("", "https://docs.bytechef.io/reference/components/github_v1#get-pull-request")
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
        .output(outputSchema(PULL_REQUEST_OUTPUT_PROPERTY))
        .perform(GithubGetPullRequestAction::perform);

    private GithubGetPullRequestAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.get(
                "/repos/%s/%s/pulls/%s".formatted(
                    inputParameters.getRequiredString(OWNER), inputParameters.getRequiredString(REPOSITORY),
                    inputParameters.getRequiredInteger(PULL_NUMBER))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

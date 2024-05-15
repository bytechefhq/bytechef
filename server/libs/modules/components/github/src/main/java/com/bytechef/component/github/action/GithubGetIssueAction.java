/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BASE_URL;
import static com.bytechef.component.github.constant.GithubConstants.GET_ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.REPO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.github.util.GithubUtils;

/**
 * @author Luka Ljubić
 */
public class GithubGetIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_ISSUE)
        .title("Get issue")
        .description("Create a specific issue")
        .properties(
            string(REPO)
                .options((ActionOptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .label("Repository")
                .description("Select an repository")
                .required(true),
            string(ISSUE)
                .options((ActionOptionsFunction<String>) GithubUtils::getIssueOptions)
                .optionsLookupDependsOn(REPO)
                .label("Issue")
                .description("Select a issue")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string("id"),
                    string("title"),
                    string("url"),
                    string("comments_url"),
                    string("number")))
        .perform(GithubGetIssueAction::perform);

    private GithubGetIssueAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context
            .http(http -> http.get(BASE_URL + "/repos/" + GithubUtils.getOwnerName(context) + "/"
                + inputParameters.getRequiredString(REPO) + "/issues/" + inputParameters.getString(ISSUE)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

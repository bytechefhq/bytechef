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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.OWNER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class GithubCreateIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createIssue")
        .title("Create Issue")
        .description("Create Issue in GitHub Repository")
        .properties(
            OWNER_PROPERTY,
            string(REPOSITORY)
                .label("Repository")
                .description("Repository where new issue will be created.")
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Title of the issue.")
                .maxLength(100)
                .required(false),
            string(BODY)
                .label("Description")
                .description("The description of the issue.")
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .perform(GithubCreateIssueAction::perform);

    private GithubCreateIssueAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "/repos/" + inputParameters.getRequiredString(OWNER) + "/"
                    + inputParameters.getRequiredString(REPOSITORY) + "/issues"))
            .body(
                Body.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    BODY, inputParameters.getString(BODY)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

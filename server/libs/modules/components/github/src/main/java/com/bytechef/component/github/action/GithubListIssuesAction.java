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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.FILTER;
import static com.bytechef.component.github.constant.GithubConstants.FILTER_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.STATE;
import static com.bytechef.component.github.constant.GithubConstants.STATE_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GithubListIssuesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listIssues")
        .title("List Issues")
        .description("Retrieve issues assigned to the authenticated user across all accessible repositories")
        .properties(
            FILTER_PROPERTY,
            STATE_PROPERTY)
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .perform(GithubListIssuesAction::perform);

    private GithubListIssuesAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.get(
                "/issues?filter=" + inputParameters.getRequiredString(FILTER) + "&state=" +
                    inputParameters.getRequiredString(STATE)))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

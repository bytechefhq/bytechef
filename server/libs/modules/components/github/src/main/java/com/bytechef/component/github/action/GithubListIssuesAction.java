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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.FILTER;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.STATE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
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
        .description("Retrieve issues assigned to the authenticated user across all accessible repositories.")
        .properties(
            string(FILTER)
                .label("Filter")
                .description("Specifies the types of issues to return.")
                .options(
                    option("Assigned", "assigned", "Issues assigned to the authenticated user."),
                    option("Created", "created", "Issues created by the authenticated user."),
                    option("Mentioned", "mentioned", "Issues mentioning the authenticated user."),
                    option("Subscribed", "subscribed",
                        "Issues the authenticated user is subscribed to updates for."),
                    option("Repos", "repos",
                        "All issues from repositories that the authenticated user has explicit access to."),
                    option("All", "all", "All issues related to the authenticated user."))
                .defaultValue("assigned")
                .required(true),
            string(STATE)
                .label("State")
                .description("Indicates the state of the issues to return.")
                .options(
                    option("Open", "open", "Open issues."),
                    option("Closed", "closed", "Closed issues."),
                    option("All", "all", "All issues."))
                .defaultValue("open")
                .required(true))
        .output(outputSchema(array().items(ISSUE_OUTPUT_PROPERTY)))
        .perform(GithubListIssuesAction::perform);

    private GithubListIssuesAction() {
    }

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/issues"))
            .queryParameters(
                Map.of(
                    FILTER, List.of(inputParameters.getRequiredString(FILTER)),
                    STATE, List.of(inputParameters.getRequiredString(STATE))))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

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

package com.bytechef.component.jira.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUES;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.jira.constant.JiraConstants.JQL;
import static com.bytechef.component.jira.constant.JiraConstants.MAX_RESULTS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class JiraSearchForIssuesUsingJqlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchForIssuesUsingJql")
        .title("Search Issues")
        .description("Search for issues using JQL")
        .properties(
            string(JQL)
                .label("JQL")
                .description(
                    "The JQL that defines the search. If no JQL expression is provided, all issues are returned.")
                .exampleValue("project = HSP")
                .required(false),
            integer(MAX_RESULTS)
                .label("Max Results")
                .description("The maximum number of items to return per page.")
                .defaultValue(50)
                .minValue(1)
                .maxValue(100)
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(ISSUE_OUTPUT_PROPERTY)))
        .perform(JiraSearchForIssuesUsingJqlAction::perform);

    private JiraSearchForIssuesUsingJqlAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> body = context.http(http -> http.get("/search"))
            .queryParameters(
                MAX_RESULTS, inputParameters.getRequiredInteger(MAX_RESULTS),
                JQL, inputParameters.getString(JQL))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get(ISSUES);
    }
}

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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUES;
import static com.bytechef.component.jira.constant.JiraConstants.JQL;
import static com.bytechef.component.jira.constant.JiraConstants.MAX_RESULTS;
import static com.bytechef.component.jira.constant.JiraConstants.NEXT_PAGE_TOKEN;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class JiraSearchForIssuesUsingJqlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchForIssuesUsingJql")
        .title("Search Issues")
        .description("Search for issues using JQL.")
        .properties(
            string(JQL)
                .label("JQL")
                .description(
                    "The JQL that defines the search. If no JQL expression is provided, all issues are returned.")
                .exampleValue("project = HSP")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string(ID)
                                    .description("The ID of the issue.")))))
        .perform(JiraSearchForIssuesUsingJqlAction::perform);

    private JiraSearchForIssuesUsingJqlAction() {
    }

    public static List<Object> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> issues = new ArrayList<>();

        String nextPageToken = null;

        do {
            Map<String, Object> body = context.http(http -> http.get("/search/jql"))
                .queryParameters(
                    MAX_RESULTS, 5000,
                    NEXT_PAGE_TOKEN, nextPageToken,
                    JQL, inputParameters.getString(JQL))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(ISSUES) instanceof List<?> list) {
                issues.addAll(list);
            }

            nextPageToken = (String) body.get(NEXT_PAGE_TOKEN);
        } while (nextPageToken != null);

        return issues;
    }
}

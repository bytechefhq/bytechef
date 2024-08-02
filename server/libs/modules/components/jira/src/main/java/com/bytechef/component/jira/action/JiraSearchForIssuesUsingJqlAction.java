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

package com.bytechef.component.jira.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUES;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.jira.constant.JiraConstants.JQL;
import static com.bytechef.component.jira.constant.JiraConstants.MAX_RESULTS;
import static com.bytechef.component.jira.constant.JiraConstants.SEARCH_FOR_ISSUES_USING_JQL;
import static com.bytechef.component.jira.util.JiraUtils.getBaseUrl;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class JiraSearchForIssuesUsingJqlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH_FOR_ISSUES_USING_JQL)
        .title("Search issues")
        .description("Search for issues using JQL")
        .properties(
            string(JQL)
                .label("JQL")
                .description(
                    "The JQL that defines the search. If no JQL expression is provided, all issues are returned")
                .exampleValue("project = HSP")
                .required(false),
            integer(MAX_RESULTS)
                .label("Max results")
                .description("The maximum number of items to return per page.")
                .defaultValue(50)
                .minValue(1)
                .maxValue(100)
                .required(true))
        .outputSchema(
            array()
                .items(ISSUE_OUTPUT_PROPERTY))
        .perform(JiraSearchForIssuesUsingJqlAction::perform);

    private JiraSearchForIssuesUsingJqlAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        StringBuilder url = new StringBuilder("?maxResults=" + inputParameters.getRequiredInteger(MAX_RESULTS));
        String jql = inputParameters.getString(JQL);

        if (jql != null) {
            url.append("&jql=")
                .append(URLEncoder.encode(jql, StandardCharsets.UTF_8));
        }

        Map<String, Object> body = context.http(http -> http.get(getBaseUrl(context) + "/search" + url))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get(ISSUES);
    }
}

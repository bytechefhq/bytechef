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
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.jira.constant.JiraConstants.ASSIGNEE;
import static com.bytechef.component.jira.constant.JiraConstants.CONTENT;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.GET_ISSUE;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_TYPE;
import static com.bytechef.component.jira.constant.JiraConstants.KEY;
import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.PRIORITY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;
import static com.bytechef.component.jira.util.JiraUtils.getBaseUrl;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.jira.util.JiraOptionsUtils;

/**
 * @author Monika Domiter
 */
public class JiraGetIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_ISSUE)
        .title("Get issue")
        .description("Get issue details in selected project.")
        .properties(
            string(PROJECT)
                .label("Project Name")
                .description("Project where the issue is located.")
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(ISSUE_ID)
                .label("Issue name")
                .options((ActionOptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string(KEY),
                    object(FIELDS)
                        .properties(
                            object(ISSUE_TYPE)
                                .properties(
                                    string(ID),
                                    string(NAME)),
                            object(PROJECT)
                                .properties(
                                    string(ID),
                                    string(NAME)),
                            object(PRIORITY)
                                .properties(
                                    string(ID),
                                    string(NAME)),
                            object(ASSIGNEE)
                                .properties(
                                    string(ID),
                                    string(NAME)),
                            object(DESCRIPTION)
                                .properties(
                                    string(TYPE),
                                    array(CONTENT)
                                        .items(
                                            object()
                                                .properties(
                                                    array(CONTENT)
                                                        .items(
                                                            object()
                                                                .properties(
                                                                    string(TEXT),
                                                                    string(TYPE))),
                                                    string(TYPE)))))))
        .perform(JiraGetIssueAction::perform);

    private JiraGetIssueAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http
                .get(getBaseUrl(connectionParameters) + "/issue/" + inputParameters.getRequiredString(ISSUE_ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

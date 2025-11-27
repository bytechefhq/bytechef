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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ACCOUNT_ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import java.util.Map;

/**
 * @author Artur Wood
 */
public class JiraAssignIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("assignIssue")
        .title("Assign Issue")
        .description("Assigns an existing issue to a specific user.")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where the issue is located.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(false),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("ID of the issue that will be assigned.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(ACCOUNT_ID)
                .label("Account ID")
                .description("ID of the account user who will be assigned the issue.")
                .options((OptionsFunction<String>) JiraOptionsUtils::getUserIdOptions)
                .required(true))
        .perform(JiraAssignIssueAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.put("/issue/" + inputParameters.getRequiredString(ISSUE_ID) + "/assignee"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(Map.of(ACCOUNT_ID, inputParameters.getRequiredString(ACCOUNT_ID))))
            .execute();

        return null;
    }
}

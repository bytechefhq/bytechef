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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.STATUS_ID;
import static com.bytechef.component.jira.constant.JiraConstants.TRANSITION;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.jira.util.JiraOptionsUtils;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class JiraTransitionIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("transitionIssue")
        .title("Transition Issue")
        .description("Move an issue to another status.")
        .properties(
            string(PROJECT)
                .label("Project ID")
                .description("ID of the project where the issue is located.")
                .options((ActionDefinition.OptionsFunction<String>) JiraOptionsUtils::getProjectIdOptions)
                .required(true),
            string(ISSUE_ID)
                .label("Issue ID")
                .description("ID of the issue to be assigned.")
                .options((ActionDefinition.OptionsFunction<String>) JiraOptionsUtils::getIssueIdOptions)
                .optionsLookupDependsOn(PROJECT)
                .required(true),
            string(STATUS_ID)
                .label("Status ID")
                .description("ID of the status you want to put the issue in.")
                .options((ActionDefinition.OptionsFunction<String>) JiraOptionsUtils::getStatusIdOptions)
                .optionsLookupDependsOn(ISSUE_ID)
                .required(true))
        .output(outputSchema(bool().description("Returns true if the issue transition was successful.")))
        .perform(JiraTransitionIssueAction::perform);

    public static Boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        context.http(
            http -> http.post("/issue/" + inputParameters.getRequiredString(ISSUE_ID) + "/transitions"))
            .body(Http.Body.of(Map.of(TRANSITION, Map.of(ID, inputParameters.getRequiredString(STATUS_ID)))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return true;
    }
}

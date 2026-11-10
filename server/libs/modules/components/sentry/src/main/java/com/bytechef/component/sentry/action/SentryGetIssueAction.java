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

package com.bytechef.component.sentry.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sentry.constant.SentryConstants.ISSUE_ID;
import static com.bytechef.component.sentry.constant.SentryConstants.ISSUE_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.sentry.util.SentryUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SentryGetIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getIssue")
        .title("Get Issue")
        .description("Get the details of an issue, such as its title, status, assignee, event count and project.")
        .properties(
            string(ISSUE_ID)
                .label("Issue ID")
                .description("The numeric ID of the issue, as shown in the issue URL in Sentry.")
                .required(true))
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/sentry_v1#get-issue")
        .perform(SentryGetIssueAction::perform);

    private SentryGetIssueAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String issuePath = SentryUtils.getIssuePath(
            connectionParameters, inputParameters.getRequiredString(ISSUE_ID));

        return context.http(http -> http.get(issuePath))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sentry.constant.SentryConstants.ASSIGNED_TO;
import static com.bytechef.component.sentry.constant.SentryConstants.ISSUE_ID;
import static com.bytechef.component.sentry.constant.SentryConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.sentry.constant.SentryConstants.STATUS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.sentry.util.SentryUtils;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SentryUpdateIssueAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateIssue")
        .title("Update Issue")
        .description(
            "Update the status or assignee of an issue. Only the fields you fill in are changed.")
        .properties(
            string(ISSUE_ID)
                .label("Issue ID")
                .description("The numeric ID of the issue, as shown in the issue URL in Sentry.")
                .required(true),
            string(STATUS)
                .label("Status")
                .description("The new status of the issue.")
                .options(
                    option("Resolved", "resolved"),
                    option("Resolved in Next Release", "resolvedInNextRelease"),
                    option("Unresolved", "unresolved"),
                    option("Ignored", "ignored"))
                .required(false),
            string(ASSIGNED_TO)
                .label("Assigned To")
                .description(
                    "The user or team to assign the issue to: a user ID, user:<user_id>, a username, a user's " +
                        "primary email, or team:<team_id>.")
                .required(false))
        .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/sentry_v1#update-issue")
        .perform(SentryUpdateIssueAction::perform);

    private SentryUpdateIssueAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String issuePath = SentryUtils.getIssuePath(
            connectionParameters, inputParameters.getRequiredString(ISSUE_ID));

        return context.http(http -> http.put(issuePath))
            .body(
                Body.of(
                    STATUS, inputParameters.getString(STATUS),
                    ASSIGNED_TO, inputParameters.getString(ASSIGNED_TO)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

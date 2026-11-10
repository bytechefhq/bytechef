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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sentry.constant.SentryConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.sentry.constant.SentryConstants.LIMIT;
import static com.bytechef.component.sentry.constant.SentryConstants.ORGANIZATION;
import static com.bytechef.component.sentry.constant.SentryConstants.PROJECT;
import static com.bytechef.component.sentry.constant.SentryConstants.QUERY;
import static com.bytechef.component.sentry.constant.SentryConstants.STATS_PERIOD;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.sentry.util.SentryUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SentryListProjectIssuesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listProjectIssues")
        .title("List Project Issues")
        .description(
            "List the issues of a project. Without a query, only unresolved issues are returned.")
        .properties(
            string(PROJECT)
                .label("Project")
                .description("The slug of the project to list issues for.")
                .options((OptionsFunction<String>) SentryUtils::getProjectOptions)
                .required(true),
            string(QUERY)
                .label("Query")
                .description(
                    "A Sentry search query, for example is:unresolved level:error. If empty, is:unresolved is " +
                        "used. Use is:resolved or is:ignored to list issues with another status.")
                .required(false),
            string(STATS_PERIOD)
                .label("Stats Period")
                .description("The period of the event counts returned for each issue.")
                .options(
                    option("24 hours", "24h"),
                    option("14 days", "14d"))
                .required(false),
            integer(LIMIT)
                .label("Limit")
                .description("The maximum number of issues to return, up to 100.")
                .minValue(1)
                .maxValue(100)
                .required(false))
        .output(outputSchema(array().items(ISSUE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/sentry_v1#list-project-issues")
        .perform(SentryListProjectIssuesAction::perform);

    private SentryListProjectIssuesAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String projectIssuesPath = "/projects/" + connectionParameters.getRequiredString(ORGANIZATION) + "/" +
            inputParameters.getRequiredString(PROJECT) + "/issues/";

        return context.http(http -> http.get(projectIssuesPath))
            .queryParameters(
                QUERY, inputParameters.getString(QUERY),
                STATS_PERIOD, inputParameters.getString(STATS_PERIOD),
                LIMIT, inputParameters.getInteger(LIMIT))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

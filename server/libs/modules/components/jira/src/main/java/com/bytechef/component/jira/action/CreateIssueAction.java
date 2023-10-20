
/*
 * Copyright 2021 <your company/name>.
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

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.component.jira.property.CreatedIssueProperties;
import com.bytechef.component.jira.property.IssueUpdateDetailsProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CreateIssueAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createIssue")
        .display(
            display("Create issue")
                .description(
                    "Creates an issue or, where the option to create subtasks is enabled in Jira, a subtask. A transition may be applied, to move the issue or subtask to a workflow step other than the default start step, and issue properties set.\n"
                        + "\n"
                        + "The content of the issue or subtask is defined using `update` and `fields`. The fields that can be set in the issue or subtask are determined using the [ Get create issue metadata](#api-rest-api-3-issue-createmeta-get). These are the same fields that appear on the issue's create screen. Note that the `description`, `environment`, and any `textarea` type custom fields (multi-line text fields) take Atlassian Document Format content. Single line custom fields (`textfield`) accept a string and don't handle Atlassian Document Format content.\n"
                        + "\n"
                        + "Creating a subtask differs from creating an issue as follows:\n"
                        + "\n"
                        + " *  `issueType` must be set to a subtask issue type (use [ Get create issue metadata](#api-rest-api-3-issue-createmeta-get) to find subtask issue types).\n"
                        + " *  `parent` must contain the ID or key of the parent issue.\n"
                        + "\n"
                        + "In a next-gen project any issue may be made a child providing that the parent and child are members of the same project.\n"
                        + "\n"
                        + "**[Permissions](#permissions) required:** *Browse projects* and *Create issues* [project permissions](https://confluence.atlassian.com/x/yodKLg) for the project in which the issue or subtask is created."))
        .metadata(
            Map.of(
                "requestMethod", "POST",
                "path", "/rest/api/3/issue", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(bool("updateHistory").label("UpdateHistory")
            .description(
                "Whether the project in which the issue is created is added to the user's **Recently viewed** project list, as shown under **Projects** in Jira. When provided, the issue type and request type are added to the user's history for a project. These values are then used to provide defaults on the issue create screen.")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            object("issueUpdateDetails").properties(IssueUpdateDetailsProperties.PROPERTIES)
                .label("IssueUpdateDetails")
                .description("Details of an issue update request.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object(null).properties(CreatedIssueProperties.PROPERTIES)
            .description("Details about a created issue or subtask.")
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .exampleOutput(
            "{\"id\":\"10000\",\"key\":\"ED-24\",\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/10000\",\"transition\":{\"status\":200,\"errorCollection\":{\"errorMessages\":[],\"errors\":{}}}}");
}

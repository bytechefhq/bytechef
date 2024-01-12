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

package com.bytechef.component.jira;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.jira.action.JiraCreateIssueAction;
import com.bytechef.component.jira.action.JiraGetIssueAction;
import com.bytechef.component.jira.action.JiraSearchForIssuesUsingJqlAction;
import com.bytechef.component.jira.connection.JiraConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractJiraComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("jira")
            .title("Jira")
            .description(
                "Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and agile project management."))
                    .actions(modifyActions(JiraCreateIssueAction.ACTION_DEFINITION,
                        JiraGetIssueAction.ACTION_DEFINITION, JiraSearchForIssuesUsingJqlAction.ACTION_DEFINITION))
                    .connection(modifyConnection(JiraConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}

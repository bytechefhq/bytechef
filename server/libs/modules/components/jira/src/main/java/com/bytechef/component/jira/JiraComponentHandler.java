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

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.jira.constant.JiraConstants.JIRA;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.jira.action.JiraCreateIssueAction;
import com.bytechef.component.jira.action.JiraGetIssueAction;
import com.bytechef.component.jira.action.JiraSearchForIssuesUsingJqlAction;
import com.bytechef.component.jira.connection.JiraConnection;
import com.bytechef.component.jira.trigger.JiraNewIssueTrigger;
import com.bytechef.component.jira.trigger.JiraUpdatedIssueTrigger;
import com.bytechef.component.jira.unifiedapi.JiraUnifiedApi;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class JiraComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(JIRA)
        .title("Jira")
        .description(
            "Jira is a proprietary issue tracking product developed by Atlassian that allows bug tracking and " +
                "agile project management.")
        .icon("path:assets/jira.svg")
        .categories(ComponentCategory.PROJECT_MANAGEMENT)
        .connection(JiraConnection.CONNECTION_DEFINITION)
        .actions(
            JiraCreateIssueAction.ACTION_DEFINITION,
            JiraGetIssueAction.ACTION_DEFINITION,
            JiraSearchForIssuesUsingJqlAction.ACTION_DEFINITION)
        .triggers(
            JiraNewIssueTrigger.TRIGGER_DEFINITION,
            JiraUpdatedIssueTrigger.TRIGGER_DEFINITION)
        .unifiedApi(JiraUnifiedApi.UNIFIED_API_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}

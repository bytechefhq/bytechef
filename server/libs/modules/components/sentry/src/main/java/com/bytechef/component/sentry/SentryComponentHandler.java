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

package com.bytechef.component.sentry;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.sentry.action.SentryGetIssueAction;
import com.bytechef.component.sentry.action.SentryGetLatestEventForIssueAction;
import com.bytechef.component.sentry.action.SentryListProjectIssuesAction;
import com.bytechef.component.sentry.action.SentryUpdateIssueAction;
import com.bytechef.component.sentry.connection.SentryConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class SentryComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("sentry")
        .title("Sentry")
        .description(
            "Sentry is an application monitoring platform that helps developers find, triage and fix errors and " +
                "performance problems in their code.")
        .icon("path:assets/sentry.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(SentryConnection.CONNECTION_DEFINITION)
        .actions(
            SentryGetIssueAction.ACTION_DEFINITION,
            SentryGetLatestEventForIssueAction.ACTION_DEFINITION,
            SentryListProjectIssuesAction.ACTION_DEFINITION,
            SentryUpdateIssueAction.ACTION_DEFINITION)
        .clusterElements(
            tool(SentryGetIssueAction.ACTION_DEFINITION),
            tool(SentryGetLatestEventForIssueAction.ACTION_DEFINITION),
            tool(SentryListProjectIssuesAction.ACTION_DEFINITION),
            tool(SentryUpdateIssueAction.ACTION_DEFINITION))
        .version(1)
        .customAction(true)
        .customActionHelp("", "https://docs.sentry.io/api/");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

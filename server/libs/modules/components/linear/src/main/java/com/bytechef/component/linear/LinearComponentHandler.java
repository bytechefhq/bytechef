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

package com.bytechef.component.linear;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.linear.action.LinearCreateCommentAction;
import com.bytechef.component.linear.action.LinearCreateIssueAction;
import com.bytechef.component.linear.action.LinearCreateProjectAction;
import com.bytechef.component.linear.action.LinearRawGraphqlQueryAction;
import com.bytechef.component.linear.action.LinearUpdateIssueAction;
import com.bytechef.component.linear.action.LinearUpdateProjectAction;
import com.bytechef.component.linear.connection.LinearConnection;
import com.bytechef.component.linear.trigger.LinearNewIssueTrigger;
import com.bytechef.component.linear.trigger.LinearRemovedIssueTrigger;
import com.bytechef.component.linear.trigger.LinearUpdatedIssueTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class LinearComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("linear")
        .title("Linear")
        .description("Linear is a project management and issue tracking tool designed primarily for software teams.")
        .icon("path:assets/linear.svg")
        .categories(ComponentCategory.PROJECT_MANAGEMENT)
        .connection(LinearConnection.CONNECTION_DEFINITION)
        .actions(
            LinearCreateIssueAction.ACTION_DEFINITION,
            LinearUpdateIssueAction.ACTION_DEFINITION,
            LinearCreateProjectAction.ACTION_DEFINITION,
            LinearUpdateProjectAction.ACTION_DEFINITION,
            LinearCreateCommentAction.ACTION_DEFINITION,
            LinearRawGraphqlQueryAction.ACTION_DEFINITION)
        .triggers(
            LinearNewIssueTrigger.TRIGGER_DEFINITION,
            LinearUpdatedIssueTrigger.TRIGGER_DEFINITION,
            LinearRemovedIssueTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(LinearCreateIssueAction.ACTION_DEFINITION),
            tool(LinearUpdateIssueAction.ACTION_DEFINITION),
            tool(LinearCreateProjectAction.ACTION_DEFINITION),
            tool(LinearUpdateProjectAction.ACTION_DEFINITION),
            tool(LinearCreateCommentAction.ACTION_DEFINITION),
            tool(LinearRawGraphqlQueryAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

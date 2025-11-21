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

package com.bytechef.component.slack;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.slack.action.SlackAddReactionAction;
import com.bytechef.component.slack.action.SlackSendApprovalMessageAction;
import com.bytechef.component.slack.action.SlackSendChannelMessageAction;
import com.bytechef.component.slack.action.SlackSendDirectMessageAction;
import com.bytechef.component.slack.connection.SlackConnection;
import com.bytechef.component.slack.trigger.SlackAnyEventTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 */
@AutoService(ComponentHandler.class)
public final class SlackComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("slack")
        .title("Slack")
        .description("Slack is a messaging platform for teams to communicate and collaborate.")
        .customAction(true)
        .icon("path:assets/slack.svg")
        .categories(ComponentCategory.COMMUNICATION, ComponentCategory.DEVELOPER_TOOLS)
        .connection(SlackConnection.CONNECTION_DEFINITION)
        .actions(
            SlackAddReactionAction.ACTION_DEFINITION,
            SlackSendApprovalMessageAction.ACTION_DEFINITION,
            SlackSendChannelMessageAction.ACTION_DEFINITION,
            SlackSendDirectMessageAction.ACTION_DEFINITION)
        .clusterElements(
            tool(SlackAddReactionAction.ACTION_DEFINITION),
            tool(SlackSendApprovalMessageAction.ACTION_DEFINITION),
            tool(SlackSendChannelMessageAction.ACTION_DEFINITION),
            tool(SlackSendDirectMessageAction.ACTION_DEFINITION))
        .triggers(SlackAnyEventTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

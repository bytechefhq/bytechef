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

package com.bytechef.component.google.chat;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.chat.action.GoogleChatCreateMessageAction;
import com.bytechef.component.google.chat.action.GoogleChatCreateSpaceAction;
import com.bytechef.component.google.chat.connection.GoogleChatConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class GoogleChatComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleChat")
        .title("Google Chat")
        .description("Google Chat is an intelligent and secure communication and collaboration tool, built for teams.")
        .icon("path:assets/google-chat.svg")
        .customAction(true)
        .customActionHelp(
            "Google Chat API documentation",
            "https://developers.google.com/workspace/chat/api/reference/rest")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(GoogleChatConnection.CONNECTION_DEFINITION)
        .actions(
            GoogleChatCreateMessageAction.ACTION_DEFINITION,
            GoogleChatCreateSpaceAction.ACTION_DEFINITION)
        .clusterElements(
            tool(GoogleChatCreateMessageAction.ACTION_DEFINITION),
            tool(GoogleChatCreateSpaceAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

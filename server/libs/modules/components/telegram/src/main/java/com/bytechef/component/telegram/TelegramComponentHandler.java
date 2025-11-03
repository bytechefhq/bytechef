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

package com.bytechef.component.telegram;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.telegram.action.TelegramSendMediaAction;
import com.bytechef.component.telegram.action.TelegramSendMessageAction;
import com.bytechef.component.telegram.connection.TelegramConnection;
import com.bytechef.component.telegram.trigger.TelegramNewMessageTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class TelegramComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("telegram")
        .title("Telegram")
        .description(
            "Telegram is a cloud-based messaging platform that enables users to send messages, media, and files, and " +
                "supports automation and integrations through its API.")
        .icon("path:assets/telegram.svg")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(TelegramConnection.CONNECTION_DEFINITION)
        .actions(
            TelegramSendMediaAction.ACTION_DEFINITION,
            TelegramSendMessageAction.ACTION_DEFINITION)
        .clusterElements(
            tool(TelegramSendMediaAction.ACTION_DEFINITION),
            tool(TelegramSendMessageAction.ACTION_DEFINITION))
        .triggers(TelegramNewMessageTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

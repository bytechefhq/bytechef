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

package com.bytechef.component.discord;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.discord.action.DiscordCreateChannelAction;
import com.bytechef.component.discord.action.DiscordSendChannelMessageAction;
import com.bytechef.component.discord.connection.DiscordConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractDiscordComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("discord")
            .title("Discord")
            .description(
                "Discord is a communication platform designed for creating communities, chatting with friends, and connecting with others through text, voice, and video channels."))
                    .actions(modifyActions(DiscordSendChannelMessageAction.ACTION_DEFINITION,
                        DiscordCreateChannelAction.ACTION_DEFINITION))
                    .connection(modifyConnection(DiscordConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}

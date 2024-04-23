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

package com.bytechef.component.microsoft.teams;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.MICROSOFT_TEAMS;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.teams.action.MicrosoftTeamsCreateChannelAction;
import com.bytechef.component.microsoft.teams.action.MicrosoftTeamsSendChannelMessageAction;
import com.bytechef.component.microsoft.teams.action.MicrosoftTeamsSendChatMessageAction;
import com.bytechef.component.microsoft.teams.connection.MicrosoftTeamsConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftTeamsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MICROSOFT_TEAMS)
        .title("Microsoft Teams")
        .description(
            "Microsoft Teams is a collaboration platform that combines workplace chat, video meetings, file storage, " +
                "and application integration.")
        .icon("path:assets/microsoft-teams.svg")
        .connection(MicrosoftTeamsConnection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftTeamsCreateChannelAction.ACTION_DEFINITION,
            MicrosoftTeamsSendChannelMessageAction.ACTION_DEFINITION,
            MicrosoftTeamsSendChatMessageAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

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

package com.bytechef.component.microsoft.teams.trigger;

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.microsoft.teams.util.MicrosoftTeamsUtils;
import com.bytechef.microsoft.commons.MicrosoftTriggerUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftTeamsNewChannelMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newChannelMessage")
        .title("New Channel Message")
        .description("Triggers when new message is received in selected channel.")
        .type(TriggerType.POLLING)
        .help(
            "",
            "https://docs.bytechef.io/reference/components/microsoft-teams_v1#new-channel-message")
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("ID of the team where the channel is located.")
                .options((OptionsFunction<String>) MicrosoftTeamsUtils::getTeamIdOptions)
                .required(true),
            string(CHANNEL_ID)
                .label("Channel ID")
                .description("Channel to monitor for new messages.")
                .optionsLookupDependsOn(TEAM_ID)
                .options((OptionsFunction<String>) MicrosoftTeamsUtils::getChannelIdOptions)
                .required(true))
        .output(outputSchema(MESSAGE_OUTPUT_PROPERTY))
        .poll(MicrosoftTeamsNewChannelMessageTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftTeamsNewChannelMessageTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String url = "/teams/%s/channels/%s/messages".formatted(
            inputParameters.getRequiredString(TEAM_ID),
            inputParameters.getRequiredString(CHANNEL_ID));

        return MicrosoftTriggerUtils.poll(url, "messageType", closureParameters, context);
    }
}

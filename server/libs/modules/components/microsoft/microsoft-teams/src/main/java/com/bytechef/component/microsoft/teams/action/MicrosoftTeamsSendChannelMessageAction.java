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

package com.bytechef.component.microsoft.teams.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.BODY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.teams.util.MicrosoftTeamsOptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftTeamsSendChannelMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendChannelMessage")
        .title("Send Channel Message")
        .description("Sends a message to a channel.")
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("ID of the team where the channel is located.")
                .options((OptionsFunction<String>) MicrosoftTeamsOptionUtils::getTeamIdOptions)
                .required(true),
            string(CHANNEL_ID)
                .label("Channel ID")
                .description("Channel to send message to.")
                .optionsLookupDependsOn(TEAM_ID)
                .options((OptionsFunction<String>) MicrosoftTeamsOptionUtils::getChannelIdOptions)
                .required(true),
            CONTENT_TYPE_PROPERTY,
            CONTENT_PROPERTY)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the message."),
                        object(BODY)
                            .description("Plaintext/HTML representation of the content of the chat message.")
                            .properties(
                                string(CONTENT_TYPE)
                                    .description("Type of the content."),
                                string(CONTENT)
                                    .description("The content of the message.")),
                        object("channelIdentity")
                            .description("Represents identity of the channel.")
                            .properties(
                                string(TEAM_ID)
                                    .description("ID of the team in which the message was posted."),
                                string(CHANNEL_ID)
                                    .description("ID of the channel in which the message was posted.")))))
        .perform(MicrosoftTeamsSendChannelMessageAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftTeamsSendChannelMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post(
                "/teams/" + inputParameters.getRequiredString(TEAM_ID) + "/channels/"
                    + inputParameters.getRequiredString(CHANNEL_ID) + "/messages"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    BODY,
                    new Object[] {
                        CONTENT, inputParameters.getRequiredString(CONTENT),
                        CONTENT_TYPE, inputParameters.getRequiredString(CONTENT_TYPE)
                    }))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

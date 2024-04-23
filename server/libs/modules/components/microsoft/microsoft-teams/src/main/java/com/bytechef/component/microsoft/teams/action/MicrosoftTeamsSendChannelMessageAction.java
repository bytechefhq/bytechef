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

package com.bytechef.component.microsoft.teams.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.BASE_URL;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.BODY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHANNEL_ID_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.SEND_CHANNEL_MESSAGE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.TEAM_ID_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class MicrosoftTeamsSendChannelMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_CHANNEL_MESSAGE)
        .title("Send channel message")
        .description("Sends a message to a channel.")
        .properties(
            TEAM_ID_PROPERTY,
            CHANNEL_ID_PROPERTY,
            CONTENT_TYPE_PROPERTY,
            CONTENT_PROPERTY)
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    object(BODY)
                        .properties(
                            string(CONTENT_TYPE),
                            string(CONTENT)),
                    object("channelIdentity")
                        .properties(
                            string(TEAM_ID),
                            string(CHANNEL_ID))))
        .perform(MicrosoftTeamsSendChannelMessageAction::perform);

    private MicrosoftTeamsSendChannelMessageAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(
                BASE_URL + "/teams/" + inputParameters.getRequiredString(TEAM_ID) + "/channels/"
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

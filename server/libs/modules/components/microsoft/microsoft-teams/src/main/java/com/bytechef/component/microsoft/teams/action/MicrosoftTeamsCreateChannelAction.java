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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DESCRIPTION;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
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
public class MicrosoftTeamsCreateChannelAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createChannel")
        .title("Create Channel")
        .description("Creates a new channel within a team.")
        .properties(
            string(TEAM_ID)
                .label("Team ID")
                .description("ID of the team where the channel will be created.")
                .options((OptionsFunction<String>) MicrosoftTeamsOptionUtils::getTeamIdOptions)
                .required(true),
            string(DISPLAY_NAME)
                .label("Channel Name")
                .maxLength(50)
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description for the channel.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the channel."),
                        dateTime("createdDateTime")
                            .description("The date and time when the channel was created."),
                        string(DISPLAY_NAME)
                            .description("Name of the channel that will appear to the user in Microsoft Teams."),
                        string(DESCRIPTION)
                            .description("Description of the channel."),
                        bool("isFavoriteByDefault")
                            .description("Indicates whether the channel is marked as favorite by default."),
                        string("webUrl")
                            .description("URL to access the channel in a web browser."),
                        bool("isArchived")
                            .description("Indicates whether the channel is archived."))))
        .perform(MicrosoftTeamsCreateChannelAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftTeamsCreateChannelAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/teams/" + inputParameters.getRequiredString(TEAM_ID) + "/channels"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

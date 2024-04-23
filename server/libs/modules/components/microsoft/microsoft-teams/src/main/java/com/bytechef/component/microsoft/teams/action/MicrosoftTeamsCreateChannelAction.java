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
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CREATE_CHANNEL;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DESCRIPTION;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;
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
public class MicrosoftTeamsCreateChannelAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CHANNEL)
        .title("Create channel")
        .description("Creates a new channel within a team.")
        .properties(
            TEAM_ID_PROPERTY,
            string(DISPLAY_NAME)
                .label("Channel name")
                .maxLength(50)
                .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description for the channel.")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string(ID),
                    string(DISPLAY_NAME),
                    string(DESCRIPTION)))
        .perform(MicrosoftTeamsCreateChannelAction::perform);

    private MicrosoftTeamsCreateChannelAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(BASE_URL + "/teams/" + inputParameters.getRequiredString(TEAM_ID) + "/channels"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    DESCRIPTION, inputParameters.getString(DESCRIPTION),
                    DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

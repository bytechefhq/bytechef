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
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CHAT_ID;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.CONTENT_TYPE_PROPERTY;
import static com.bytechef.component.microsoft.teams.constant.MicrosoftTeamsConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.teams.util.MicrosoftTeamsOptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Domiter
 */
public class MicrosoftTeamsSendChatMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendChatMessage")
        .title("Send Chat Message")
        .description("Sends a message in an existing chat.")
        .properties(
            string(CHAT_ID)
                .label("Chat ID")
                .options((ActionOptionsFunction<String>) MicrosoftTeamsOptionUtils::getChatIdOptions)
                .required(true),
            CONTENT_TYPE_PROPERTY,
            CONTENT_PROPERTY)
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the message."),
                        string("chatId")
                            .description("ID of the chat."),
                        object(BODY)
                            .description("Plaintext/HTML representation of the content of the chat message.")
                            .properties(
                                string(CONTENT_TYPE)
                                    .description("Type of the content."),
                                string(CONTENT)
                                    .description("The content of the message.")))))
        .perform(MicrosoftTeamsSendChatMessageAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftTeamsSendChatMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/chats/" + inputParameters.getRequiredString(CHAT_ID) + "/messages"))
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

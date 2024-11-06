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

package com.bytechef.component.discord.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.discord.constant.DiscordConstants.CONTENT;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID_PROPERTY;
import static com.bytechef.component.discord.constant.DiscordConstants.RECIPIENT_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.TTS;
import static com.bytechef.component.discord.util.DiscordUtils.getDMChannel;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.discord.util.DiscordUtils;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class DiscordSendDirectMessageAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendDirectMessage")
        .title("Send Direct Message")
        .description("Send direct message guild member.")
        .properties(
            GUILD_ID_PROPERTY,
            string(RECIPIENT_ID)
                .label("Recipient")
                .description("The recipient to open a DM channel with.")
                .optionsLookupDependsOn(GUILD_ID)
                .options((ActionOptionsFunction<String>) DiscordUtils::getGuildMemberIdOptions)
                .required(true),
            string(CONTENT)
                .label("Message Text")
                .description("Message contents (up to 2000 characters)")
                .required(true),
            bool(TTS)
                .label("Text to Speech")
                .description("True if this is a TTS message")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("body")
                            .properties(
                                string("id")))))
        .perform(DiscordSendDirectMessageAction::perform);

    private DiscordSendDirectMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> body = getDMChannel(inputParameters, actionContext);

        return actionContext.http(http -> http.post("/channels/" + body.get("id") + "/messages"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    CONTENT, inputParameters.getRequiredString(CONTENT),
                    TTS, inputParameters.getBoolean(TTS)))
            .execute()
            .getBody(new TypeReference<>() {});
    }

}

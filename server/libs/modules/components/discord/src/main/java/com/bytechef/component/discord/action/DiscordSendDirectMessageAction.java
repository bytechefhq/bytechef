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

package com.bytechef.component.discord.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
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
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
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
                .options((OptionsFunction<String>) DiscordUtils::getGuildMemberIdOptions)
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
                        integer("type")
                            .description("Type of the message.")
                            .required(false),
                        string("id")
                            .description("ID of the message.")
                            .required(false),
                        string("content")
                            .description("Contents of the message.")
                            .required(false),
                        bool("tts")
                            .description("Whether this was a TTS message.")
                            .required(false),
                        array("mentions")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the user.")
                                            .required(false),
                                        string("username")
                                            .description("Username of the user.")
                                            .required(false))
                                    .description("Users specifically mentioned in the message."))
                            .description("Users specifically mentioned in the message.")
                            .required(false),
                        array("mention_roles")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the role.")
                                            .required(false),
                                        string("name")
                                            .description("Name of the role.")
                                            .required(false))
                                    .description("Roles specifically mentioned in this message."))
                            .description("Roles specifically mentioned in this message.")
                            .required(false),
                        array("attachments")
                            .items(
                                object()
                                    .properties(
                                        string("id")
                                            .description("ID of the attachment.")
                                            .required(false),
                                        string("filename")
                                            .description("Name of the file attached.")
                                            .required(false),
                                        string("title")
                                            .description("Title of the file.")
                                            .required(false),
                                        string("description")
                                            .description("Description of the file.")
                                            .required(false),
                                        string("content_type")
                                            .description("The attachment's media type.")
                                            .required(false),
                                        integer("size")
                                            .description("Size of the file in bytes.")
                                            .required(false),
                                        string("url")
                                            .description("Source url of file.")
                                            .required(false),
                                        string("proxy_url")
                                            .description("A proxied url of file.")
                                            .required(false))
                                    .description("Any attached files."))
                            .description("Any attached files.")
                            .required(false),
                        string("timestamp")
                            .description("When this message was sent.")
                            .required(false),
                        integer("flags")
                            .description("message flags combined as a bitfield.")
                            .required(false),
                        array("components")
                            .items(
                                object()
                                    .description(
                                        "Sent if the message contains components like buttons, action rows, or other interactive components."))
                            .description(
                                "Sent if the message contains components like buttons, action rows, or other interactive components.")
                            .required(false),
                        string("channel_id")
                            .description("ID of the channel the message was sent in.")
                            .required(false),
                        object("author")
                            .properties(
                                string("id")
                                    .description("ID of the author.")
                                    .required(false),
                                string("username")
                                    .description("Username of the author.")
                                    .required(false))
                            .description("The author of this message.")
                            .required(false),
                        bool("pinned")
                            .description("Whether this message is pinned.")
                            .required(false),
                        bool("mention_everyone")
                            .description("Whether this message mentions everyone.")
                            .required(false))))
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

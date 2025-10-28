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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.discord.util.DiscordUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class DiscordSendChannelMessageAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("sendChannelMessage")
        .title("Send Channel Message")
        .description("Post a new message to a specific #channel you choose.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/channels/{channelId}/messages", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("channelId").label("Channel ID")
            .description("ID of the channel where to send the message.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) DiscordUtils::getChannelIdOptions)
            .optionsLookupDependsOn("guildId")
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("content").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Message Text")
                .description("Message contents (up to 2000 characters)")
                .required(true),
            bool("tts").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Text to Speech")
                .description("True if this is a TTS message")
                .defaultValue(false)
                .required(false))
        .output(outputSchema(object().properties(integer("type").description("Type of the message.")
            .required(false),
            string("id").description("ID of the message.")
                .required(false),
            string("content").description("Contents of the message.")
                .required(false),
            bool("tts").description("Whether this was a TTS message.")
                .required(false),
            array("mentions").items(object().properties(string("id").description("ID of the user.")
                .required(false),
                string("username").description("Username of the user.")
                    .required(false))
                .description("Users specifically mentioned in the message."))
                .description("Users specifically mentioned in the message.")
                .required(false),
            array("mention_roles").items(object().properties(string("id").description("ID of the role.")
                .required(false),
                string("name").description("Name of the role.")
                    .required(false))
                .description("Roles specifically mentioned in this message."))
                .description("Roles specifically mentioned in this message.")
                .required(false),
            array("attachments").items(object().properties(string("id").description("ID of the attachment.")
                .required(false),
                string("filename").description("Name of the file attached.")
                    .required(false),
                string("title").description("Title of the file.")
                    .required(false),
                string("description").description("Description of the file.")
                    .required(false),
                string("content_type").description("The attachment's media type.")
                    .required(false),
                integer("size").description("Size of the file in bytes.")
                    .required(false),
                string("url").description("Source url of file.")
                    .required(false),
                string("proxy_url").description("A proxied url of file.")
                    .required(false))
                .description("Any attached files."))
                .description("Any attached files.")
                .required(false),
            string("timestamp").description("When this message was sent.")
                .required(false),
            integer("flags").description("message flags combined as a bitfield.")
                .required(false),
            array("components").items(object().description(
                "Sent if the message contains components like buttons, action rows, or other interactive components."))
                .description(
                    "Sent if the message contains components like buttons, action rows, or other interactive components.")
                .required(false),
            string("channel_id").description("ID of the channel the message was sent in.")
                .required(false),
            object("author").properties(string("id").description("ID of the author.")
                .required(false),
                string("username").description("Username of the author.")
                    .required(false))
                .description("The author of this message.")
                .required(false),
            bool("pinned").description("Whether this message is pinned.")
                .required(false),
            bool("mention_everyone").description("Whether this message mentions everyone.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private DiscordSendChannelMessageAction() {
    }
}

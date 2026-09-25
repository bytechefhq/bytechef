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

package com.bytechef.component.discord.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.discord.constant.DiscordConstants.CHANNEL_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.GUILD_ID;
import static com.bytechef.component.discord.constant.DiscordConstants.LAST_MESSAGE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.discord.util.DiscordUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class DiscordNewMessageTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newMessage")
        .title("New Message")
        .description("Triggers when a new message is sent in a specific channel.")
        .type(TriggerType.POLLING)
        .properties(
            string(GUILD_ID)
                .label("Guild ID")
                .description("ID of the guild where the channel is located.")
                .options((OptionsFunction<String>) DiscordUtils::getGuildIdOptions)
                .required(true),
            string(CHANNEL_ID)
                .label("Channel ID")
                .description("ID of the channel to monitor for new messages.")
                .optionsLookupDependsOn(GUILD_ID)
                .options((OptionsFunction<String>) DiscordUtils::getChannelIdOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("type").description("Type of the message."),
                        string("id").description("ID of the message."),
                        string("content").description("Contents of the message."),
                        bool("tts").description("Whether this was a TTS message."),
                        array("mentions")
                            .description("Users specifically mentioned in the message.")
                            .items(
                                object()
                                    .properties(
                                        string("id").description("ID of the user."),
                                        string("username").description("Username of the user."))),
                        array("mention_roles")
                            .description("Roles specifically mentioned in this message.")
                            .items(
                                object()
                                    .properties(
                                        string("id").description("ID of the role."),
                                        string("name").description("Name of the role."))),
                        array("attachments")
                            .description("Any attached files.")
                            .items(
                                object()
                                    .properties(
                                        string("id").description("ID of the attachment."),
                                        string("filename").description("Name of the file attached."),
                                        string("title").description("Title of the file."),
                                        string("description").description("Description of the file."),
                                        string("content_type").description("The attachment's media type."),
                                        integer("size").description("Size of the file in bytes."),
                                        string("url").description("Source url of file."),
                                        string("proxy_url").description("A proxied url of file."))),
                        string("timestamp").description("When this message was sent."),
                        integer("flags").description("Message flags combined as a bitfield."),
                        string("channel_id").description("ID of the channel the message was sent in."),
                        object("author")
                            .description("The author of this message.")
                            .properties(
                                string("id").description("ID of the author."),
                                string("username").description("Username of the author.")),
                        bool("pinned").description("Whether this message is pinned."),
                        bool("mention_everyone").description("Whether this message mentions everyone."))))
        .poll(DiscordNewMessageTrigger::poll);

    private DiscordNewMessageTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String channelId = inputParameters.getRequiredString(CHANNEL_ID);
        String lastMessageId = closureParameters.getString(LAST_MESSAGE_ID);
        boolean editorEnvironment = context.isEditorEnvironment();

        List<Map<String, ?>> messages;

        if (lastMessageId == null) {
            messages = fetchMessages(context, channelId, null, editorEnvironment ? 1 : 100);

            if (messages.isEmpty()) {
                return new PollOutput(List.of(), Map.of(), false);
            }

            Map<String, ?> latestMessage = Collections.max(
                messages, Comparator.comparing(message -> String.valueOf(message.get("id"))));

            String latestMessageId = String.valueOf(latestMessage.get("id"));

            if (editorEnvironment) {
                return new PollOutput(List.of(latestMessage), Map.of(LAST_MESSAGE_ID, latestMessageId), false);
            }

            return new PollOutput(List.of(), Map.of(LAST_MESSAGE_ID, latestMessageId), false);
        }

        messages = fetchMessages(context, channelId, lastMessageId, 100);

        if (messages.isEmpty()) {
            return new PollOutput(List.of(), Map.of(LAST_MESSAGE_ID, lastMessageId), false);
        }

        List<Map<String, ?>> sortedMessages = new ArrayList<>(messages);

        sortedMessages.sort(Comparator.comparing(message -> String.valueOf(message.get("id"))));

        Map<String, ?> newestMessage = sortedMessages.get(sortedMessages.size() - 1);

        String newestMessageId = String.valueOf(newestMessage.get("id"));

        return new PollOutput(sortedMessages, Map.of(LAST_MESSAGE_ID, newestMessageId), false);
    }

    private static List<Map<String, ?>> fetchMessages(
        TriggerContext context, String channelId, String afterMessageId, int limit) {

        return context.http(http -> http.get("/channels/" + channelId + "/messages"))
            .queryParameters(
                afterMessageId == null
                    ? new Object[] {
                        "limit", limit
                    }
                    : new Object[] {
                        "limit", limit, "after", afterMessageId
                    })
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

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

package com.bytechef.component.telegram.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Property.ValueProperty;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class TelegramConstants {

    public static final String BOT_TOKEN = "botToken";
    public static final String CHAT_ID = "chat_id";
    public static final String DIRECT_MESSAGES_TOPIC_ID = "direct_messages_topic_id";
    public static final String DOCUMENT = "document";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String PHOTO = "photo";
    public static final String TEXT = "text";
    public static final String VIDEO = "video";

    private static final List<ValueProperty<?>> CHAT_OUTPUT_PROPERTIES = List.of(
        integer("id")
            .description("Unique identifier for this chat."),
        string("title")
            .description("Title, for supergroups, channels and group chats."),
        string("username")
            .description("Username, for private chats, supergroups and channels if available."),
        string("type")
            .description("Type of the chat, can be either “private”, “group”, “supergroup” or “channel”."),
        bool("is_direct_messages")
            .description("True, if the chat is the direct messages chat of a channel."));

    private static final List<ValueProperty<?>> USER_OUTPUT_PROPERTIES = List.of(
        integer("id")
            .description("Unique identifier for this user or bot. "),
        bool("is_bot")
            .description("True, if this user is a bot."),
        string("first_name")
            .description("User's or bot's first name."),
        string("last_name")
            .description("User's or bot's last name."),
        string("username")
            .description("User's or bot's username"));

    public static final List<ValueProperty<?>> PHOTO_OUTPUT_PROPERTIES = List.of(
        string("file_id")
            .description("Identifier for this file, which can be used to download or reuse the file."),
        string("file_unique_id")
            .description(
                "Unique identifier for this file, which is supposed to be the same over time and for different bots. Can't be used to download or reuse the file."),
        integer("width")
            .description("Photo width."),
        integer("height")
            .description("Photo height."),
        integer("file_size")
            .description("File size in bytes."));

    public static final List<ValueProperty<?>> MESSAGE_OUTPUT_PROPERTIES = List.of(
        integer("message_id")
            .description("Unique message identifier inside chat."),
        integer("message_thread_id")
            .description(
                "Unique identifier of a message thread to which the message belongs; for supergroups only"),
        object("direct_messages_topic")
            .description("Information about the direct messages chat topic that contains the message")
            .properties(
                integer("topic_id")
                    .description("Unique identifier of the topic."),
                object("user")
                    .description("Information about the user that created the topic.")
                    .properties(USER_OUTPUT_PROPERTIES)),
        object("from")
            .description("Sender of the message; may be empty for messages sent to channels.")
            .properties(USER_OUTPUT_PROPERTIES),
        object("sender_chat")
            .description("Sender of the message when sent on behalf of a chat.")
            .properties(CHAT_OUTPUT_PROPERTIES),
        integer("date")
            .description("Date the message was sent in Unix time."),
        object("chat")
            .properties(CHAT_OUTPUT_PROPERTIES),
        string("text")
            .description("For text messages, the actual UTF-8 text of the message."),
        object("direct_messages_topic")
            .description("Information about the direct messages chat topic that contains the message.")
            .properties(
                integer("topic_id")
                    .description("Unique identifier of the topic."),
                object("user")
                    .properties(USER_OUTPUT_PROPERTIES)),
        object("document")
            .description("Message is a general file, information about the file.")
            .properties(
                string("file_id")
                    .description("Identifier for this file, which can be used to download or reuse the file."),
                string("file_unique_id")
                    .description(
                        "Unique identifier for this file, which is supposed to be the same over time and for different bots. Can't be used to download or reuse the file."),
                object("thumbnail")
                    .description("Document thumbnail as defined by the sender.")
                    .properties(PHOTO_OUTPUT_PROPERTIES),
                string("file_name")
                    .description("Original filename as defined by the sender."),
                string("mime_type")
                    .description("MIME type of the file as defined by the sender."),
                integer("file_size")
                    .description("File size in bytes.")),
        array("photo")
            .description("Message is a photo, available sizes of the photo.")
            .items(object().properties(PHOTO_OUTPUT_PROPERTIES)),
        object("video")
            .description("Message is a video, information about the video.")
            .properties(
                string("file_id")
                    .description("Identifier for this file, which can be used to download or reuse the file."),
                string("file_unique_id")
                    .description(
                        "Unique identifier for this file, which is supposed to be the same over time and for different bots. Can't be used to download or reuse the file."),
                string("width")
                    .description("Video width as defined by the sender."),
                string("height")
                    .description("Video height as defined by the sender."),
                integer("duration")
                    .description("Duration of the video in seconds as defined by the sender."),
                string("file_name")
                    .description("Original filename as defined by the sender."),
                string("mime_type")
                    .description("MIME type of the file as defined by the sender."),
                integer("file_size")
                    .description("File size in bytes.")));

    private TelegramConstants() {
    }
}

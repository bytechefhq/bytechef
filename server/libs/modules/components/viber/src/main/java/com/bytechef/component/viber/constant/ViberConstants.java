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

package com.bytechef.component.viber.constant;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Property.ValueProperty;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ViberConstants {

    public static final String AUTH_TOKEN = "authToken";
    public static final String CONVERSATION_STARTED = "conversation_started";
    public static final String EVENTS = "events";
    public static final String MESSAGE = "message";
    public static final String RECEIVER = "receiver";
    public static final String RECEIVERS = "receivers";
    public static final String SENDER_AVATAR = "senderAvatar";
    public static final String SENDER_NAME = "senderName";
    public static final int SENDER_NAME_MAX_LENGTH = 28;
    public static final String SUBSCRIBED = "subscribed";
    public static final String TEXT = "text";
    public static final String TRACKING_DATA = "trackingData";
    public static final String UNSUBSCRIBED = "unsubscribed";
    public static final String WEBHOOK = "webhook";
    public static final String WELCOME_MESSAGE = "welcomeMessage";
    public static final String WELCOME_SENDER_NAME = "welcomeSenderName";

    public static final List<ValueProperty<?>> EVENT_OUTPUT_PROPERTIES = List.of(
        string("event")
            .description("The Viber event type."),
        integer("timestamp")
            .description("Time of the event in epoch milliseconds."),
        integer("message_token")
            .description("Unique ID of the message."),
        object("user")
            .description("The Viber user the event relates to.")
            .properties(
                string("id")
                    .description("Unique Viber user ID."),
                string("name")
                    .description("The user's Viber name."),
                string("avatar")
                    .description("URL of the user's avatar."),
                string("country")
                    .description("Two-letter country code."),
                string("language")
                    .description("The user's phone language."),
                integer("api_version")
                    .description("Viber API version supported by the user's client.")),
        object("message")
            .description("The message sent by the user.")
            .properties(
                string("type")
                    .description("Message type, such as text, picture, video, file, sticker, contact or location."),
                string("text")
                    .description("Text of the message."),
                string("media")
                    .description("URL of the attached media."),
                string("thumbnail")
                    .description("URL of the media thumbnail."),
                string("file_name")
                    .description("Name of the attached file."),
                integer("file_size")
                    .description("Size of the attached file in bytes."),
                integer("duration")
                    .description("Duration of the video in seconds."),
                integer("sticker_id")
                    .description("Viber sticker ID."),
                object("location")
                    .description("Shared location.")
                    .properties(
                        number("lat")
                            .description("Latitude."),
                        number("lon")
                            .description("Longitude.")),
                object("contact")
                    .description("Shared contact.")
                    .properties(
                        string("name")
                            .description("Contact name."),
                        string("phone_number")
                            .description("Contact phone number."),
                        string("avatar")
                            .description("URL of the contact avatar.")),
                string("tracking_data")
                    .description("Tracking data attached to the previous message of the bot.")),
        fileEntry("file")
            .description("The downloaded media of a picture, video or file message."),
        string("type")
            .description("Conversation started type, such as open."),
        string("context")
            .description("Additional data passed when the conversation was opened."),
        bool("subscribed")
            .description("Whether the user is already subscribed."));

    private ViberConstants() {
    }
}

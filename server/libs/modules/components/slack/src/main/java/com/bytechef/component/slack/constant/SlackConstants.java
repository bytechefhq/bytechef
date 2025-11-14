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

package com.bytechef.component.slack.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
public class SlackConstants {

    public static final String CHANNEL = "channel";
    public static final String CHALLENGE = "challenge";
    public static final String ERROR = "error";
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String OK = "ok";
    public static final String TEXT = "text";
    public static final String TYPE = "type";

    public static final ModifiableObjectProperty CHAT_POST_MESSAGE_RESPONSE_PROPERTY = object()
        .properties(
            bool(OK)
                .description("Indicates if the message was successfully sent."),
            string(CHANNEL)
                .description("ID of the channel the message was sent to."),
            string("ts")
                .description("Timestamp of the message."),
            object("message")
                .description("The sent message.")
                .properties(
                    string("user")
                        .description("ID of the user who sent the message."),
                    string(TYPE)
                        .description("Type of the message."),
                    string("ts")
                        .description("Timestamp of the message."),
                    string("text")
                        .description("The text of the message."),
                    string("team")
                        .description("ID of the team the message was sent to."),
                    string("subtype")),
            string("warning"),
            object("responseMetadata")
                .properties(
                    array("messages")
                        .items(string())));

    public static final ModifiableStringProperty TEXT_PROPERTY = string(TEXT)
        .label("Message")
        .description("The text of your message.")
        .controlType(ControlType.TEXT_AREA)
        .required(true);

    private SlackConstants() {
    }
}

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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class DiscordSendChannelMessageAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("sendChannelMessage")
        .title("Send channel message")
        .description("Post a new message to a specific #channel you choose.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/channels/{channelId}/messages", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("channelId").label("Channel")
            .description("Channel where to send the message")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(string("content").label("Message   Text")
                .description("Message contents (up to 2000 characters)")
                .required(true),
                bool("tts").label("Text   To   Speech")
                    .description("True if this is a TTS message")
                    .defaultValue(false)
                    .required(false))
                .label("Message")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false), string("content").required(false),
                    bool("tts").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private DiscordSendChannelMessageAction() {
    }
}

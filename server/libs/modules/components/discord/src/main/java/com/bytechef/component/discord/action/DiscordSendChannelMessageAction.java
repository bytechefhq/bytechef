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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.OptionsDataSource;
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
            .options((OptionsDataSource.ActionOptionsFunction<String>) DiscordUtils::getChannelIdOptions)
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
        .output(outputSchema(object()
            .properties(string("id").required(false), string("content").required(false), bool("tts").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private DiscordSendChannelMessageAction() {
    }
}

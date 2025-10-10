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
import static com.bytechef.component.definition.ComponentDsl.option;
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
public class DiscordCreateChannelAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createChannel")
        .title("Create Channel")
        .description("Create a new channel")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/guilds/{guildId}/channels", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("guildId").label("Guild ID")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) DiscordUtils::getGuildIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("name").minLength(1)
                .maxLength(100)
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Name")
                .description("The name of the new channel")
                .required(true),
            integer("type").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Type")
                .options(option("0", 0), option("2", 2), option("4", 4))
                .required(false))
        .output(outputSchema(object().properties(string("id").description("ID of the channel.")
            .required(false),
            integer("type").description("Type of the channel.")
                .required(false),
            string("last_message_id").description("ID of the last message sent in this channel.")
                .required(false),
            integer("flags").description("Channel flags combined as a bitfield.")
                .required(false),
            string("guild_id").description("ID of the guild to which the channel belongs.")
                .required(false),
            string("name").description("Name of the channel.")
                .required(false),
            string("parent_id").description("For guild channels: id of the parent category for a channel")
                .required(false),
            integer("rate_limit_per_user")
                .description("Amount of seconds a user has to wait before sending another message")
                .required(false),
            string("topic").description("Topic of the channel.")
                .required(false),
            integer("position")
                .description("Sorting position of the channel (channels with the same position are sorted by id)")
                .required(false),
            array("permission_overwrites")
                .items(object().properties(string("id").description("ID of the role or user this overwrite applies to.")
                    .required(false),
                    integer("type").description("Type of overwrite, 0 for role, 1 for member.")
                        .required(false),
                    string("allow").description("Permissions allowed by this overwrite.")
                        .required(false),
                    string("deny").description("Permissions denied by this overwrite.")
                        .required(false))
                    .description("Explicit permission overwrites for members and roles."))
                .description("Explicit permission overwrites for members and roles.")
                .required(false),
            bool("nsfw").description("Whether the channel is marked as NSFW (Not Safe For Work).")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private DiscordCreateChannelAction() {
    }
}

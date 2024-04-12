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
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
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
public class DiscordCreateChannelAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createChannel")
        .title("Create channel")
        .description("Create a new channel")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/guilds/{guildId}/channels", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("guildId").label("Guild")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(string("name").minLength(1)
                .maxLength(100)
                .label("Name")
                .description("The name of the new channel")
                .required(true),
                integer("type").label("Type")
                    .options(option("0", 0), option("2", 2), option("4", 4))
                    .required(false))
                .label("Channel")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false), integer("type").required(false),
                    string("name").required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private DiscordCreateChannelAction() {
    }
}

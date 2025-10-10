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

package com.bytechef.component.discord.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.discord.util.DiscordUtils;

/**
 * @author Monika Domiter
 */
public class DiscordConstants {

    public static final String CONTENT = "content";
    public static final String GUILD_ID = "guildId";
    public static final String RECIPIENT_ID = "recipient_id";
    public static final String TTS = "tts";

    public static final ModifiableStringProperty GUILD_ID_PROPERTY = string(GUILD_ID)
        .label("Guild ID")
        .options((OptionsFunction<String>) DiscordUtils::getGuildIdOptions)
        .required(true);

    private DiscordConstants() {
    }
}

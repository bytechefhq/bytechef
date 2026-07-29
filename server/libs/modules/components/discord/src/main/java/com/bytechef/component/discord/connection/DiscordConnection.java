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

package com.bytechef.component.discord.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.discord.constant.DiscordConstants.PUBLIC_KEY;

import com.bytechef.component.definition.ComponentDsl;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class DiscordConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://discord.com/api/v10")
        .authorizations(authorization(AuthorizationType.BEARER_TOKEN)
            .title("Bearer Token")
            .properties(
                string(TOKEN)
                    .label("Bot Token")
                    .required(true),
                string(PUBLIC_KEY)
                    .label("Application Public Key")
                    .description(
                        "The Discord app's public key. When set, field-less approval requests use in-place " +
                            "Approve/Discard interaction buttons resolved through the app's Interactions Endpoint " +
                            "URL (<public-url>/discord/interactivity), verified by the Ed25519 request signature. " +
                            "Also set bytechef.webhook.discord.public-key to the same value. Leave empty to deliver " +
                            "approval links to the hosted form.")
                    .required(false)));

    private DiscordConnection() {
    }
}

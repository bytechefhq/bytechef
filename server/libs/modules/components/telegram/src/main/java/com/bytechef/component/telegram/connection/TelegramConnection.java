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

package com.bytechef.component.telegram.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.telegram.constant.TelegramConstants.BOT_TOKEN;
import static com.bytechef.component.telegram.constant.TelegramConstants.WEBHOOK_SECRET_TOKEN;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Kušter
 */
public class TelegramConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.telegram.org/bot"
            + connectionParameters.getRequiredString(BOT_TOKEN))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(BOT_TOKEN)
                        .label("Bot Token")
                        .required(true),
                    string(WEBHOOK_SECRET_TOKEN)
                        .label("Webhook Secret Token")
                        .description(
                            "Optional secret token set when registering this bot's webhook (setWebhook) at " +
                                "<public-url>/telegram/interactivity. When set, field-less approval requests use " +
                                "in-place Approve/Discard buttons resolved through that webhook, verified against " +
                                "this token. Use a bot dedicated to approvals, since a bot has a single webhook. " +
                                "Leave empty to deliver approval links to the hosted form.")
                        .required(false)))
        .help("", "https://docs.bytechef.io/reference/components/telegram_v1#connection-setup")
        .version(1);

    private TelegramConnection() {
    }
}

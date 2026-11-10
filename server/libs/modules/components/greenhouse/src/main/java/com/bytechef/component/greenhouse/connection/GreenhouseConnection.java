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

package com.bytechef.component.greenhouse.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.TOKEN_URL;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public class GreenhouseConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://harvest.greenhouse.io/v3")
        .help("", "https://docs.bytechef.io/reference/components/greenhouse_v1#connection-setup")
        .version(1)
        .authorizations(
            authorization(AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                .title("Harvest V3 (OAuth)")
                .properties(
                    string(CLIENT_ID)
                        .label("Client ID")
                        .description("The Client ID of your Harvest V3 (OAuth) API credentials.")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .description("The Client Secret of your Harvest V3 (OAuth) API credentials.")
                        .required(true),
                    string(TOKEN_URL)
                        .label("Token URL")
                        .description("The Greenhouse OAuth token endpoint.")
                        .defaultValue("https://auth.greenhouse.io/token")
                        .required(true)));

    private GreenhouseConnection() {
    }
}

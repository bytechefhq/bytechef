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

package com.bytechef.component.wrike.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Nikolina Spehar
 */
public class WrikeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://www.wrike.com/api/v4")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .properties(
                    string(CLIENT_ID)
                        .label("Client ID")
                        .description("Client ID of your Wrike app.")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .description("Client Secret of your Wrike app.")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://login.wrike.com/oauth2/authorize/v4")
                .tokenUrl((connectionParameters, context) -> "https://login.wrike.com/oauth2/token")
                .refreshUrl((connectionParameters, context) -> "https://login.wrike.com/oauth2/token"));

    private WrikeConnection() {
    }
}

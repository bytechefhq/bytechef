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

package com.bytechef.component.snowflake.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.ACCOUNT_IDENTIFIER;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SnowflakeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://%s.snowflakecomputing.com/api/v2"
            .formatted(connectionParameters.getRequiredString(ACCOUNT_IDENTIFIER)))
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .properties(
                    string(ACCOUNT_IDENTIFIER)
                        .label("Account Identifier")
                        .description("Account identifier of your account.")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client ID")
                        .description("Snowflake OAuth Client ID.")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .description("Snowflake OAuth Client Secret.")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://%s.snowflakecomputing.com/oauth/authorize"
                    .formatted(connectionParameters.getRequiredString(ACCOUNT_IDENTIFIER)))
                .scopes((connection, context) -> Map.of("refresh_token", true))
                .tokenUrl((connectionParameters, context) -> "https://%s.snowflakecomputing.com/oauth/token-request"
                    .formatted(connectionParameters.getRequiredString(ACCOUNT_IDENTIFIER)))
                .refreshUrl((connectionParameters, context) -> "https://%s.snowflakecomputing.com/oauth/token-request"
                    .formatted(connectionParameters.getRequiredString(ACCOUNT_IDENTIFIER)))
                .apply((connectionParameters, context) -> ApplyResponse.ofHeaders(
                    Map.of(
                        "Accept", List.of("application/json"),
                        AUTHORIZATION, List.of("Bearer " + connectionParameters.getRequiredString(ACCESS_TOKEN))))));

    private SnowflakeConnection() {
    }
}

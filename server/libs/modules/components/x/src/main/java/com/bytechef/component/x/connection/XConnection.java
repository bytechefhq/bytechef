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

package com.bytechef.component.x.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class XConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.x.com/2")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://x.com/i/oauth2/authorize")
                .refreshUrl((connection, context) -> "https://api.x.com/2/oauth2/token")
                .scopes((connection, context) -> List.of(
                    "tweet.read", "tweet.write", "users.read", "media.write", "like.write", "offline.access"))
                .tokenUrl((connection, context) -> "https://api.x.com/2/oauth2/token")
                .pkce((verifier, challenge, challengeMethod, context) -> new Authorization.Pkce(
                    "challenge", null, "plain"))
                .oAuth2AuthorizationExtraQueryParameters(
                    (connection, context) -> Map.of("code_challenge", "challenge", "code_challenge_method", "plain")));

    private XConnection() {
    }
}

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

package com.bytechef.component.spotify.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class SpotifyConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.spotify.com/v1")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://accounts.spotify.com/authorize")
            .scopes((connectionParameters, context) -> {
                Map<String, Boolean> scopeMap = new HashMap<>();

                scopeMap.put("ugc-image-upload", false);
                scopeMap.put("user-read-playback-state", false);
                scopeMap.put("user-modify-playback-state", true);
                scopeMap.put("user-read-currently-playing", false);
                scopeMap.put("app-remote-control", false);
                scopeMap.put("streaming", false);
                scopeMap.put("playlist-read-private", true);
                scopeMap.put("playlist-read-collaborative", false);
                scopeMap.put("playlist-modify-private", true);
                scopeMap.put("playlist-modify-public", true);
                scopeMap.put("user-follow-modify", false);
                scopeMap.put("user-follow-read", false);
                scopeMap.put("user-read-playback-position", false);
                scopeMap.put("user-top-read", false);
                scopeMap.put("user-read-recently-played", false);
                scopeMap.put("user-library-modify", false);
                scopeMap.put("user-library-read", false);
                scopeMap.put("user-read-email", false);
                scopeMap.put("user-read-private", false);
                scopeMap.put("user-personalized", false);
                scopeMap.put("user-soa-link", false);
                scopeMap.put("user-soa-unlink", false);
                scopeMap.put("soa-manage-entitlements", false);
                scopeMap.put("soa-manage-partner", false);
                scopeMap.put("soa-create-partner", false);

                return scopeMap;
            })
            .tokenUrl((connectionParameters, context) -> "https://accounts.spotify.com/api/token")
            .refreshUrl((connectionParameters, context) -> "https://accounts.spotify.com/api/token"));

    private SpotifyConnection() {
    }
}

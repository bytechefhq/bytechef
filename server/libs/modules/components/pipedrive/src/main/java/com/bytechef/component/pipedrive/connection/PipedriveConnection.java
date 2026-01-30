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

package com.bytechef.component.pipedrive.connection;

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
public class PipedriveConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.pipedrive.com/v1")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://oauth.pipedrive.com/oauth/authorize")
            .scopes((connectionParameters, context) -> {
                Map<String, Boolean> scopeMap = new HashMap<>();

                scopeMap.put("deals:read", false);
                scopeMap.put("deals:full", false);
                scopeMap.put("goals:read", false);
                scopeMap.put("goals:full", false);
                scopeMap.put("leads:read", false);
                scopeMap.put("leads:full", false);
                scopeMap.put("activities:read", false);
                scopeMap.put("activities:full", false);
                scopeMap.put("contacts:read", false);
                scopeMap.put("contacts:full", false);
                scopeMap.put("admin", false);
                scopeMap.put("recents:read", false);
                scopeMap.put("search:read", false);
                scopeMap.put("mail:read", false);
                scopeMap.put("mail:full", false);
                scopeMap.put("products:read", false);
                scopeMap.put("products:full", false);
                scopeMap.put("users:read", false);
                scopeMap.put("base", false);
                scopeMap.put("phone-integration", false);

                return scopeMap;
            })
            .tokenUrl((connectionParameters, context) -> "https://oauth.pipedrive.com/oauth/token")
            .refreshUrl((connectionParameters, context) -> "https://oauth.pipedrive.com/oauth/token"));

    private PipedriveConnection() {
    }
}

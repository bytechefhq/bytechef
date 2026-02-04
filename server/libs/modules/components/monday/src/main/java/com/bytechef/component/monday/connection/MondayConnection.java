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

package com.bytechef.component.monday.connection;

import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MondayConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://auth.monday.com/oauth2/authorize")
            .scopes((connection, context) -> {
                Map<String, Boolean> map = new HashMap<>();

                map.put("account:read", false);
                map.put("assets:read", false);
                map.put("boards:read", true);
                map.put("boards:write", true);
                map.put("docs:read", false);
                map.put("docs:write", false);
                map.put("me:read", false);
                map.put("notifications:write", false);
                map.put("tags:read", false);
                map.put("teams:read", false);
                map.put("teams:write", false);
                map.put("updates:read", false);
                map.put("updates:write", false);
                map.put("users:read", false);
                map.put("users:write", false);
                map.put("webhooks:read", false);
                map.put("webhooks:write", true);
                map.put("workspaces:read", true);
                map.put("workspaces:write", false);

                return map;
            })
            .tokenUrl((connectionParameters, context) -> "https://auth.monday.com/oauth2/token")
            .refreshUrl((connectionParameters, context) -> "https://auth.monday.com/oauth2/token"))
        .baseUri((connectionParameters, context) -> "https://api.monday.com/v2");

    private MondayConnection() {
    }
}

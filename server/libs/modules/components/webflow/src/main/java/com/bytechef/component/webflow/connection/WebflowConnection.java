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

package com.bytechef.component.webflow.connection;

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
public class WebflowConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.webflow.com/v2")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://webflow.com/oauth/authorize")
            .scopes((connectionParameters, context) -> {
                Map<String, Boolean> scopeMap = new HashMap<>();

                scopeMap.put("assets:read", false);
                scopeMap.put("assets:write", false);
                scopeMap.put("authorized_user:read", false);
                scopeMap.put("cms:read", true);
                scopeMap.put("cms:write", false);
                scopeMap.put("comments:read", false);
                scopeMap.put("comments:write", false);
                scopeMap.put("components:read", false);
                scopeMap.put("components:write", false);
                scopeMap.put("custom_code:read", false);
                scopeMap.put("custom_code:write", false);
                scopeMap.put("ecommerce:read", true);
                scopeMap.put("ecommerce:write", true);
                scopeMap.put("forms:read", false);
                scopeMap.put("forms:write", false);
                scopeMap.put("pages:read", false);
                scopeMap.put("pages:write", false);
                scopeMap.put("sites:read", true);
                scopeMap.put("sites:write", false);
                scopeMap.put("site_activity:read", false);
                scopeMap.put("site_config:read", false);
                scopeMap.put("site_config:write", false);
                scopeMap.put("users:read", false);
                scopeMap.put("users:write", false);
                scopeMap.put("workspace:read", false);
                scopeMap.put("workspace:write", false);

                return scopeMap;
            })
            .tokenUrl((connectionParameters, context) -> "https://api.webflow.com/oauth/access_token")
            .refreshUrl((connectionParameters, context) -> "https://api.webflow.com/oauth/access_token"));

    private WebflowConnection() {
    }
}

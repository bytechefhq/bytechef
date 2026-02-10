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

package com.bytechef.component.productboard.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.TOKEN;
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
public class ProductboardConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.productboard.com")
        .authorizations(authorization(AuthorizationType.BEARER_TOKEN)
            .title("Bearer Token")
            .properties(
                string(TOKEN)
                    .label("Token")
                    .required(true)),
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://app.productboard.com/oauth2/authorize")
                .scopes((connectionParameters, context) -> {
                    Map<String, Boolean> scopeMap = new HashMap<>();

                    scopeMap.put("custom_fields:read", false);
                    scopeMap.put("members_pii:read", false);
                    scopeMap.put("notes:create", true);
                    scopeMap.put("notes:read", true);
                    scopeMap.put("notes:manage", true);
                    scopeMap.put("companies:read", false);
                    scopeMap.put("plugin_integrations:manage", false);
                    scopeMap.put("product_hierarchy_data:create", false);
                    scopeMap.put("product_hierarchy_data:manage", false);
                    scopeMap.put("product_hierarchy_data:read", true);
                    scopeMap.put("releases:create", false);
                    scopeMap.put("releases:manage", false);
                    scopeMap.put("releases:read", false);
                    scopeMap.put("users:manage", false);
                    scopeMap.put("users:read", false);
                    scopeMap.put("users_pii:read", false);
                    scopeMap.put("objectives:read", false);
                    scopeMap.put("objectives:create", false);
                    scopeMap.put("objectives:manage", false);
                    scopeMap.put("key_results:read", false);
                    scopeMap.put("key_results:create", false);
                    scopeMap.put("key_results:manage", false);
                    scopeMap.put("initiatives:read", false);
                    scopeMap.put("initiatives:create", false);
                    scopeMap.put("initiatives:manage", false);
                    scopeMap.put("feedback_form_configurations:read", false);
                    scopeMap.put("feedback_forms:create", false);

                    return scopeMap;
                })
                .tokenUrl((connectionParameters, context) -> "https://app.productboard.com/oauth2/token")
                .refreshUrl((connectionParameters, context) -> "https://app.productboard.com/oauth2/token"));

    private ProductboardConnection() {
    }
}

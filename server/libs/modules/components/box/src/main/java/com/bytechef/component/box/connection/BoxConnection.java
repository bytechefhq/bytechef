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

package com.bytechef.component.box.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class BoxConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.box.com/2.0")
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://account.box.com/api/oauth2/authorize")
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("root_readonly", true);
                    map.put("root_readwrite", true);
                    map.put("manage_managed_users", false);
                    map.put("manage_app_users", false);
                    map.put("manage_groups", false);
                    map.put("manage_webhook", true);
                    map.put("manage_enterprise_properties", false);
                    map.put("manage_data_retention", false);
                    map.put("ai.readwrite", false);
                    map.put("sign_requests.readwrite", false);
                    map.put("manage_triggers", false);
                    map.put("manage_legal_holds", false);
                    map.put("enterprise_content", false);

                    return map;
                })
                .tokenUrl((connectionParameters, context) -> "https://api.box.com/oauth2/token")
                .refreshUrl((connectionParameters, context) -> "https://api.box.com/oauth2/token"));

    private BoxConnection() {
    }
}
